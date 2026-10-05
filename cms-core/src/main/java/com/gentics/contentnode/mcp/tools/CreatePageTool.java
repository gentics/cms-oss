package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.PageInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.util.MiscUtils;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Creates a page as an offline draft. Delegates to {@link PageResourceImpl#create}, which checks the permission to
 * create pages in the folder and leaves the new page locked, so the tool releases the lock and reads the page back.
 * A language the node does not offer (which the CMS ignores silently) and a template not linked to the folder (which
 * the CMS reports as a general failure) are rejected before.
 */
public class CreatePageTool extends AbstractMcpTool {
	static final String NAME = "create_page";

	static final String ARG_FOLDER_ID = "folderId";

	static final String ARG_TEMPLATE_ID = "templateId";

	static final String ARG_NAME = "name";

	static final String ARG_LANGUAGE = "language";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_FILE_NAME = "fileName";

	static final String ARG_DESCRIPTION = "description";

	static final String ARG_NICE_URL = "niceUrl";

	static final String ARG_PRIORITY = "priority";

	static final String ARG_FAIL_ON_DUPLICATE = "failOnDuplicate";

	static final String ARG_FORCE_EXTENSION = "forceExtension";

	/**
	 * Result of the tool
	 * @param page the new page
	 * @param created always true
	 */
	public record Result(PageInfo page, boolean created) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_FOLDER_ID, schema("integer", "ID of the folder to create the page in.", "minimum", 1));
		properties.put(ARG_TEMPLATE_ID, schema("integer", "ID of a template linked to the folder.", "minimum", 1));
		properties.put(ARG_NAME, schema("string", "Name of the page.", "minLength", 1, "maxLength", 255));
		properties.put(ARG_LANGUAGE, schema("string", "Language code, one of the node's languages.", "minLength", 2,
				"maxLength", 10));
		properties.put(ARG_NODE_ID, schema("integer", "Not supported yet: calls that set it are rejected.", "minimum",
				1));
		properties.put(ARG_FILE_NAME, schema("string",
				"Filename. The template's extension is appended if missing, unless forceExtension.", "maxLength", 255));
		properties.put(ARG_DESCRIPTION, schema("string", "Description.", "maxLength", 4000));
		properties.put(ARG_NICE_URL, schema("string", "Nice URL, ignored if the feature is off.", "maxLength", 500));
		properties.put(ARG_PRIORITY, schema("integer", "Priority.", "minimum", 1, "maximum", 100));
		properties.put(ARG_FAIL_ON_DUPLICATE, schema("boolean",
				"Fail if the name or filename is already used, instead of making it unique.", "default", true));
		properties.put(ARG_FORCE_EXTENSION, schema("boolean",
				"Keep fileName as given, without the template's extension.", "default", false));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Create page")
				.description("Creates a new page as an OFFLINE draft in a folder, from a template. The page is never "
						+ "published by this tool; publishing is a separate, explicit step. Resolve folderId, "
						+ "templateId and language first (get_folder_tree, get_template, list_nodes) and check "
						+ "permission with get_permissions. Do NOT call it twice for the same page: with "
						+ "failOnDuplicate it errors on a duplicate filename instead of creating a second page.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_FOLDER_ID, ARG_TEMPLATE_ID, ARG_NAME, ARG_LANGUAGE))
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Writes.rejectChannel(NAME, arguments);
		PageCreateRequest request = request(arguments);
		int folderId = Integer.parseInt(request.getFolderId());
		Writes.logIdempotencyKey(NAME, folderId, arguments);

		checkLanguageAndTemplate(folderId, request.getLanguage(), request.getTemplateId());

		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());
		PageLoadResponse response = pageResource.create(request);
		requireOk(response, "The page was not created in folder %d".formatted(folderId));
		// create() leaves the new page locked
		int id = response.getPage().getId();
		releaseLock(Page.class, Integer.toString(id));

		return new Result(PageInfo.of(Writes.readBack(pageResource, id)), true);
	}

	/**
	 * Validate the arguments and build the create request
	 * @param arguments arguments
	 * @return create request
	 * @throws IllegalArgumentException for a missing or invalid argument
	 */
	static PageCreateRequest request(Map<String, Object> arguments) {
		int folderId = Args.id(arguments, ARG_FOLDER_ID);
		int templateId = Args.id(arguments, ARG_TEMPLATE_ID);
		String name = required(arguments, ARG_NAME, 1, 255);
		String language = required(arguments, ARG_LANGUAGE, 2, 10);

		PageCreateRequest request = new PageCreateRequest();
		request.setFolderId(Integer.toString(folderId));
		request.setTemplateId(templateId);
		request.setPageName(name);
		request.setLanguage(language);
		request.setFileName(stringArg(arguments, ARG_FILE_NAME, 0, 255));
		request.setDescription(stringArg(arguments, ARG_DESCRIPTION, 0, 4000));
		request.setNiceUrl(stringArg(arguments, ARG_NICE_URL, 0, 500));
		request.setPriority(intArg(arguments, ARG_PRIORITY, 1, 100));
		// set explicitly, the CMS defaults to false
		request.setFailOnDuplicate(booleanArg(arguments, ARG_FAIL_ON_DUPLICATE, true));
		request.setForceExtension(booleanArg(arguments, ARG_FORCE_EXTENSION, false));
		return request;
	}

	/**
	 * Get a required string argument
	 * @param arguments arguments
	 * @param name argument name
	 * @param minLength minimum length
	 * @param maxLength maximum length
	 * @return value
	 * @throws IllegalArgumentException if the argument is missing or invalid
	 */
	private static String required(Map<String, Object> arguments, String name, int minLength, int maxLength) {
		String value = stringArg(arguments, name, minLength, maxLength);
		if (value == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(name));
		}
		return value;
	}

	/**
	 * Check that the folder's node offers the language and that the template is linked to the folder. A folder the
	 * caller cannot view is left to the delegate, which answers with the right problem.
	 * @param folderId folder ID
	 * @param language language code
	 * @param templateId template ID
	 * @throws Exception
	 * @throws IllegalArgumentException if the language or the template does not fit the folder
	 */
	private static void checkLanguageAndTemplate(int folderId, String language, int templateId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			Folder folder = t.getObject(Folder.class, folderId);
			if (folder == null || !t.canView(folder)) {
				return;
			}
			Folder master = folder.getMaster();

			if (MiscUtils.getRequestedContentLanguage(master, language) == null) {
				List<String> codes = master.getNode().getLanguages().stream().map(ContentLanguage::getCode).toList();
				throw new IllegalArgumentException("The node of folder %d has no language '%s', it has %s"
						.formatted(folderId, language, codes));
			}

			Template template = t.getObject(Template.class, templateId);
			if (template == null
					|| template.getFolders().stream().noneMatch(f -> f.getId().equals(master.getId()))) {
				throw new IllegalArgumentException("Template %d is not linked to folder %d, see list_templates"
						.formatted(templateId, folderId));
			}
			trx.success();
		}
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("page", PageInfo.jsonSchema("The new page, offline and unlocked."));
		properties.put("created", schema("boolean", "Always true."));
		return schema("object", null, "properties", properties, "required", List.of("page", "created"));
	}
}
