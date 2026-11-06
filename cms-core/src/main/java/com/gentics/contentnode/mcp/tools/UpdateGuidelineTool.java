package com.gentics.contentnode.mcp.tools;

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
import com.gentics.contentnode.mcp.util.Guidelines.Guideline;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Replaces a guideline's text or priority. Delegates to {@link PackageResourceImpl}, whose class-level permissions
 * apply.
 */
public class UpdateGuidelineTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	static final String ARG_CONTENT = "content";

	static final String ARG_PRIORITY = "priority";

	/**
	 * Result of the tool
	 * @param id ID of the guideline
	 */
	public record Result(String id) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, GetGuidelineTool.idSchema());
		properties.put(ARG_CONTENT, schema("string", "The new Markdown text.", "minLength", 1));
		properties.put(ARG_PRIORITY, schema("integer", "The new priority.", "minimum", 0));

		return Tool.builder().name("update_guideline").title("Update guideline")
				.description("Replaces a guideline's text or priority. The change applies at once. Changing its name "
						+ "or default makes it another guideline, which is a create and a delete.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_ID))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", Map.of("id", schema("string", null)), "required",
						List.of("id")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		String id = stringArg(arguments, ARG_ID, 1, 512);
		String content = stringArg(arguments, ARG_CONTENT, 1, Integer.MAX_VALUE);
		Integer priority = intArg(arguments, ARG_PRIORITY, 0, Integer.MAX_VALUE);
		if (id == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(ARG_ID));
		}
		if (content == null && priority == null) {
			throw new IllegalArgumentException(
					"Nothing to change: give '%s' or '%s'".formatted(ARG_CONTENT, ARG_PRIORITY));
		}

		ListPackagesTool.checkDevtools();
		PackageResource resource = RestPermissions.guard(PackageResource.class, new PackageResourceImpl());
		Guideline guideline = Guidelines.find(resource, id);
		String text = content != null ? content : Guidelines.read(resource, guideline);
		String fileName = priority != null
				? Guidelines.fileName(guideline.name(), guideline.isDefault(), priority)
				: guideline.fileName();

		// the new file first: a failure leaves a duplicate ID, which resolves to one file, not a missing guideline
		Guidelines.write(resource, guideline.packageName(), fileName, text);
		if (!fileName.equals(guideline.fileName())) {
			Guidelines.delete(resource, guideline);
		}
		return new Result(id);
	}
}
