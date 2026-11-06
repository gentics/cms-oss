package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpArgs.booleanArg;
import static com.gentics.contentnode.mcp.McpArgs.intArg;
import static com.gentics.contentnode.mcp.McpArgs.stringArg;
import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.util.Guidelines;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Writes a new guideline file in a package. Delegates to {@link PackageResourceImpl}, whose class-level permissions
 * apply.
 */
public class CreateGuidelineTool extends AbstractMcpTool {
	static final String ARG_PACKAGE = "package";

	static final String ARG_NAME = "name";

	static final String ARG_DEFAULT = "default";

	static final String ARG_PRIORITY = "priority";

	static final String ARG_CONTENT = "content";

	/**
	 * Result of the tool
	 * @param id ID of the new guideline
	 */
	public record Result(String id) {
	}

	/**
	 * Request of the tool
	 * @param packageName package
	 * @param name name
	 * @param isDefault whether it is a default guideline
	 * @param priority priority, may be null
	 * @param content text
	 */
	record Request(String packageName, String name, boolean isDefault, Integer priority, String content) {
		/**
		 * Parse the arguments
		 * @param arguments arguments
		 * @return request
		 * @throws IllegalArgumentException if an argument is missing or invalid
		 */
		static Request of(Map<String, Object> arguments) {
			String packageName = stringArg(arguments, ARG_PACKAGE, 1, 255);
			String name = stringArg(arguments, ARG_NAME, 1, 128);
			String content = stringArg(arguments, ARG_CONTENT, 1, Integer.MAX_VALUE);
			if (packageName == null || name == null || content == null) {
				throw new IllegalArgumentException("Arguments '%s', '%s' and '%s' are required".formatted(ARG_PACKAGE,
						ARG_NAME, ARG_CONTENT));
			}
			if (!name.matches(Guidelines.NAME_PATTERN)) {
				throw new IllegalArgumentException("Argument '%s' must match %s, but was '%s'".formatted(ARG_NAME,
						Guidelines.NAME_PATTERN, name));
			}
			return new Request(packageName, name, booleanArg(arguments, ARG_DEFAULT, false),
					intArg(arguments, ARG_PRIORITY, 0, Integer.MAX_VALUE), content);
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PACKAGE, schema("string", "The devtools package.", "minLength", 1));
		properties.put(ARG_NAME, schema("string", "Letters, digits, _ and -, not starting with a priority.", "pattern",
				Guidelines.NAME_PATTERN));
		properties.put(ARG_DEFAULT, schema("boolean", "Applies to every session of the package's nodes.", "default",
				false));
		properties.put(ARG_PRIORITY, schema("integer", "Lower comes first. Not part of the ID.", "minimum", 0));
		properties.put(ARG_CONTENT, schema("string", "The Markdown text.", "minLength", 1));

		return Tool.builder().name("create_guideline").title("Create guideline")
				.description("Writes a new guideline file in a package. Refused with invalid-data when the ID already "
						+ "exists, also with another priority. default: true applies it to every session of the "
						+ "package's nodes, so say so to the user, and name the nodes when the package has several.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_PACKAGE, ARG_NAME, ARG_CONTENT)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", Map.of("id", schema("string", null)), "required",
						List.of("id")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Request request = Request.of(arguments);
		String id = Guidelines.id(request.packageName(), request.name(), request.isDefault());

		ListPackagesTool.checkDevtools();
		PackageResource resource = RestPermissions.guard(PackageResource.class, new PackageResourceImpl());
		if (Guidelines.inPackage(resource, request.packageName()).stream().anyMatch(g -> g.id().equals(id))) {
			throw new IllegalArgumentException("Guideline '%s' already exists".formatted(id));
		}
		Guidelines.write(resource, request.packageName(),
				Guidelines.fileName(request.name(), request.isDefault(), request.priority()), request.content());
		return new Result(id);
	}
}
