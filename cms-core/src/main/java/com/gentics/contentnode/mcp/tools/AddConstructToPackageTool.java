package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.rest.exceptions.DuplicateEntityException;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Adds a construct to a devtools package, which writes it into the package directory. Delegates to
 * {@link PackageResourceImpl#addConstruct}, which needs the view permission on devtools packages but checks nothing
 * on the construct, so the tool checks the devtools feature and the edit permission on the construct. A construct
 * already in the package is reported with {@code added: false}.
 */
public class AddConstructToPackageTool extends AbstractMcpTool {
	static final String NAME = "add_construct_to_package";

	static final String ARG_PACKAGE_NAME = "packageName";

	static final String ARG_CONSTRUCT_ID = "constructId";

	static final String ARG_CONSTRUCT_KEYWORD = "constructKeyword";

	/**
	 * Result of the tool
	 * @param packageName package name
	 * @param constructRef construct
	 * @param added whether the construct was added by this call
	 */
	public record Result(String packageName, ObjectRef constructRef, boolean added) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PACKAGE_NAME, schema("string", "Name of the package, see list_packages.", "minLength", 1,
				"maxLength", 100));
		properties.put(ARG_CONSTRUCT_ID, schema("integer", "ID of the construct.", "minimum", 1));
		properties.put(ARG_CONSTRUCT_KEYWORD, schema("string", "Keyword of the construct.", "maxLength", 100));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		Map<String, Object> outputProperties = new LinkedHashMap<>();
		outputProperties.put("packageName", schema("string", null));
		outputProperties.put("constructRef", objectRefSchema(null));
		outputProperties.put("added", schema("boolean", "false if the construct was already in the package."));

		return Tool.builder().name(NAME).title("Add construct to package")
				.description("Adds an existing construct to a devtools package so it becomes version-controllable and "
						+ "promotable between environments. The CMS writes the construct into the package directory "
						+ "right away; synchronising the package with other environments is left to a human and is "
						+ "not available as a tool. Pass exactly one of constructId or constructKeyword. Needs the "
						+ "devtools feature and the right to edit the construct.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_PACKAGE_NAME)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", outputProperties, "required", List.of("packageName",
						"added")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		String packageName = nullIfBlank(stringArg(arguments, ARG_PACKAGE_NAME, 1, 100));
		if (packageName == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_PACKAGE_NAME));
		}
		int id = GetConstructTool.constructId(arguments, ARG_CONSTRUCT_ID, ARG_CONSTRUCT_KEYWORD);
		Writes.logIdempotencyKey(NAME, id, arguments);
		ListPackagesTool.checkDevtools();

		ObjectRef constructRef;
		try (Trx trx = ContentNodeHelper.trx()) {
			Construct construct = trx.getTransaction().getObject(Construct.class, id);
			if (construct == null) {
				throw new EntityNotFoundException("Construct %d does not exist".formatted(id));
			}
			if (!trx.getTransaction().getPermHandler().canEdit(construct)) {
				throw new InsufficientPrivilegesException("Missing permission to edit construct %d".formatted(id),
						construct, PermType.update);
			}
			constructRef = new ObjectRef(ObjectRef.Type.CONSTRUCT, id, construct.getGlobalId().toString(), null,
					construct.getName().toString(), null, null, null, null);
			trx.success();
		}

		boolean added = true;
		try {
			RestPermissions.guard(PackageResource.class, new PackageResourceImpl()).addConstruct(packageName,
					Integer.toString(id));
		} catch (DuplicateEntityException e) {
			added = false;
		}
		return new Result(packageName, constructRef, added);
	}
}
