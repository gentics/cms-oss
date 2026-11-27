package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Tasks;
import com.gentics.contentnode.rest.resource.TaskResource;
import com.gentics.contentnode.rest.resource.impl.TaskResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Deletes a stored task, addressed by its ID or its external key. Delegates to {@link TaskResourceImpl#delete},
 * which only lets the creator delete.
 */
public class DeleteTaskTool extends AbstractMcpTool {
	static final String NAME = "delete_task";

	/**
	 * Result of the tool
	 * @param deleted false if no task had the given external key, so there was nothing to delete
	 * @param id ID of the deleted task
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(boolean deleted, String id) {
	}

	@Override
	public Tool tool() {
		return Tool.builder().name(NAME).title("Delete task")
				.description("Deletes a task you created, addressed by its id or by its externalKey. Deleting by "
						+ "an externalKey that no task has is not an error: the result says deleted false. Tasks "
						+ "the CMS derives itself, such as the publish queue, cannot be deleted.")
				.inputSchema(JsonSchema.builder().type("object").properties(Tasks.addressProperties())
						.required(List.of()).additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		TaskResource resource = RestPermissions.guard(TaskResource.class, new TaskResourceImpl());
		String id = Tasks.resolveId(resource, arguments);
		if (id == null) {
			return new Result(false, null);
		}
		requireOk(resource.delete(id), "Task %s was not deleted".formatted(id));
		return new Result(true, id);
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("deleted", schema("boolean", "False if no task had the given externalKey."));
		properties.put("id", schema("string", "ID of the deleted task."));
		return schema("object", null, "properties", properties, "required", List.of("deleted"));
	}
}
