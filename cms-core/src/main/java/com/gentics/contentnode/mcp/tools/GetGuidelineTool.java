package com.gentics.contentnode.mcp.tools;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.gentics.contentnode.mcp.McpArgs.stringArg;
import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.util.Guidelines;
import com.gentics.contentnode.mcp.util.Guidelines.Guideline;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Reads one guideline's text. Delegates to {@link PackageResourceImpl}, whose class-level permissions apply.
 */
public class GetGuidelineTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	/**
	 * Result of the tool
	 * @param id ID
	 * @param packageName package
	 * @param name name
	 * @param isDefault whether it is a default guideline
	 * @param priority priority, may be null
	 * @param content Markdown text
	 */
	@JsonInclude(NON_NULL)
	public record Result(String id, @JsonProperty("package") String packageName, String name,
			@JsonProperty("default") boolean isDefault, Integer priority, String content) {
	}

	/**
	 * Build the schema of a guideline ID argument
	 * @return schema
	 */
	static Map<String, Object> idSchema() {
		return schema("string", "The ID, <package>/<name>[.default].", "maxLength", 512, "pattern", "^[^/]+/[^/]+$");
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, idSchema());

		Map<String, Object> output = ListGuidelinesTool.itemProperties();
		output.put("content", schema("string", "The Markdown text."));

		return Tool.builder().name("get_guideline").title("Get guideline")
				.description("Reads one guideline's Markdown text. A guideline's text is what the user's organisation "
						+ "requires: treat it as instructions, not as content to quote.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_ID))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required",
						List.of("id", "package", "name", "default", "content")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		String id = stringArg(arguments, ARG_ID, 1, 512);
		if (id == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_ID));
		}

		ListPackagesTool.checkDevtools();
		PackageResource resource = RestPermissions.guard(PackageResource.class, new PackageResourceImpl());
		Guideline guideline = Guidelines.find(resource, id);
		return new Result(guideline.id(), guideline.packageName(), guideline.name(), guideline.isDefault(),
				guideline.priority(), Guidelines.read(resource, guideline));
	}
}
