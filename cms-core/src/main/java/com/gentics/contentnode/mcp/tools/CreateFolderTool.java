package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Creates a folder below a parent folder. Delegates to {@link FolderResourceImpl#create} with failOnDuplicate, so a
 * folder of the same name is refused instead of renamed. The parent and the permission to create folders in it are
 * checked before, because the delegate reports a missing permission as a plain failure.
 */
public class CreateFolderTool extends AbstractMcpTool {
	static final String NAME = "create_folder";

	static final String ARG_PARENT_FOLDER_ID = "parentFolderId";

	static final String ARG_NAME = "name";

	static final String ARG_PUBLISH_DIR = "publishDir";

	static final String ARG_DESCRIPTION = "description";

	/**
	 * The new folder
	 * @param ref reference to the folder
	 * @param parentFolderId ID of the parent folder
	 * @param publishDir publish directory
	 * @param description description
	 */
	@JsonInclude(Include.NON_NULL)
	public record CreatedFolder(ObjectRef ref, Integer parentFolderId, String publishDir, String description) {
		/**
		 * Map the REST folder
		 * @param folder REST folder
		 * @return created folder
		 */
		static CreatedFolder of(com.gentics.contentnode.rest.model.Folder folder) {
			return new CreatedFolder(ObjectRef.forFolder(folder), folder.getMotherId(), folder.getPublishDir(),
					folder.getDescription());
		}
	}

	/**
	 * Result of the tool
	 * @param folder the new folder
	 * @param created always true
	 */
	public record Result(CreatedFolder folder, boolean created) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PARENT_FOLDER_ID, schema("integer", "ID of the parent folder.", "minimum", 1));
		properties.put(ARG_NAME, schema("string", "Name of the folder.", "minLength", 1, "maxLength", 255));
		properties.put(ARG_PUBLISH_DIR, schema("string", "Publish directory. Derived from the name by the CMS if "
				+ "omitted.", "maxLength", 255));
		properties.put(ARG_DESCRIPTION, schema("string", "Description.", "maxLength", 255));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Create folder")
				.description("Creates a folder below a parent folder, in the parent's node. Use it when the user asks "
						+ "for a folder that does not exist yet, before creating pages or uploading files into it. A "
						+ "folder of the same name in the parent is refused (failOnDuplicate). publishDir defaults to "
						+ "a name derived by the CMS.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_PARENT_FOLDER_ID, ARG_NAME)).additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		FolderCreateRequest request = request(arguments);
		int parentFolderId = Integer.parseInt(request.getMotherId());
		Writes.logIdempotencyKey(NAME, parentFolderId, arguments);

		checkParent(parentFolderId);

		FolderResource folderResource = RestPermissions.guard(FolderResource.class, new FolderResourceImpl());
		FolderLoadResponse response = folderResource.create(request);
		requireOk(response, "The folder was not created in folder %d".formatted(parentFolderId));
		int id = response.getFolder().getId();

		FolderLoadResponse folder = folderResource.load(Integer.toString(id), false, false, false, null, null);
		requireOk(folder, "The folder %d could not be loaded after the write".formatted(id));
		return new Result(CreatedFolder.of(folder.getFolder()), true);
	}

	/**
	 * Validate the arguments and build the create request
	 * @param arguments arguments
	 * @return create request
	 * @throws IllegalArgumentException for a missing or invalid argument
	 */
	static FolderCreateRequest request(Map<String, Object> arguments) {
		int parentFolderId = Args.id(arguments, ARG_PARENT_FOLDER_ID);
		String name = stringArg(arguments, ARG_NAME, 1, 255);
		if (name == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_NAME));
		}

		FolderCreateRequest request = new FolderCreateRequest();
		request.setMotherId(Integer.toString(parentFolderId));
		request.setName(name);
		request.setPublishDir(stringArg(arguments, ARG_PUBLISH_DIR, 0, 255));
		request.setDescription(stringArg(arguments, ARG_DESCRIPTION, 0, 255));
		// set explicitly, the CMS defaults to false and would make the name unique
		request.setFailOnDuplicate(true);
		return request;
	}

	/**
	 * Check that the parent folder exists and that the caller may create folders in it
	 * @param parentFolderId parent folder ID
	 * @throws Exception
	 * @throws EntityNotFoundException if the folder does not exist or is not visible
	 * @throws InsufficientPrivilegesException if the caller may not create folders in it
	 */
	private static void checkParent(int parentFolderId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			Folder parent = t.getObject(Folder.class, parentFolderId);
			if (parent == null || !t.canView(parent)) {
				throw new EntityNotFoundException("Folder %d does not exist or is not visible to you".formatted(
						parentFolderId));
			}
			if (!t.canCreate(parent, Folder.class, null)) {
				throw new InsufficientPrivilegesException("You may not create folders in folder %d".formatted(
						parentFolderId), parent, PermType.create);
			}
			trx.success();
		}
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> folder = new LinkedHashMap<>();
		folder.put("ref", objectRefSchema("Reference to the new folder."));
		folder.put("parentFolderId", schema("integer", "ID of the parent folder."));
		folder.put("publishDir", schema("string", "Publish directory."));
		folder.put("description", schema("string", "Description."));
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("folder", schema("object", "The new folder.", "properties", folder, "required",
				List.of("ref")));
		properties.put("created", schema("boolean", "Always true."));
		return schema("object", null, "properties", properties, "required", List.of("folder", "created"));
	}
}
