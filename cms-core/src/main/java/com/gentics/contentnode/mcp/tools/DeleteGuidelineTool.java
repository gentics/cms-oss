package com.gentics.contentnode.mcp.tools;

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
 * Deletes a guideline file. Delegates to {@link PackageResourceImpl}, whose class-level permissions apply.
 */
public class DeleteGuidelineTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	/**
	 * Result of the tool
	 * @param id ID of the deleted guideline
	 * @param deleted always true
	 */
	public record Result(String id, boolean deleted) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, GetGuidelineTool.idSchema());

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("id", schema("string", null));
		output.put("deleted", schema("boolean", "Always true."));

		return Tool.builder().name("delete_guideline").title("Delete guideline")
				.description("Deletes a guideline file. This is permanent, so confirm with the user first. The harness "
						+ "also calls it when a plan that created the guideline is discarded.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_ID))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("id", "deleted")))
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
		Guidelines.delete(resource, Guidelines.find(resource, id));
		return new Result(id, true);
	}
}
