package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpArgs.stringArg;
import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Tasks;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.model.task.UserTaskResponse;
import com.gentics.contentnode.rest.resource.TaskResource;
import com.gentics.contentnode.rest.resource.impl.TaskResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Creates a stored task, or updates the task that already has the given external key. Delegates to
 * {@link TaskResourceImpl#create}, which checks that the caller and the assignee can view the objects of the task.
 */
public class CreateTaskTool extends AbstractMcpTool {
	static final String NAME = "create_task";

	/**
	 * Result of the tool
	 * @param task the task as stored
	 */
	public record Result(UserTaskModel task) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>(Tasks.taskProperties());
		properties.put(Tasks.ARG_SOURCE, schema("string", "Where the task comes from. manual if omitted.", "enum",
				Tasks.SOURCES));
		properties.put(Tasks.ARG_EXTERNAL_KEY, schema("string", "Key of the task in its source. A task with this "
				+ "source and key that you created is updated instead of a second one being created.", "minLength",
				1, "maxLength", 255));

		return Tool.builder().name(NAME).title("Create task")
				.description("Creates a task for yourself, another user or a group. With an externalKey the call "
						+ "is repeatable: the task with that source and key is updated. A key that another user "
						+ "created a task with is refused.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of("type", "title")).additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		stringArg(arguments, "type", 1, 255);
		stringArg(arguments, "title", 1, 255);
		String source = stringArg(arguments, Tasks.ARG_SOURCE, 1, 32);
		if (source != null) {
			Tasks.source(source);
		}
		UserTaskModel task = Tasks.model(arguments);

		TaskResource resource = RestPermissions.guard(TaskResource.class, new TaskResourceImpl());
		UserTaskResponse response = resource.create(task);
		requireOk(response, "The task was not saved");
		return new Result(response.getTask());
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("task", Tasks.taskSchema("The task as stored."));
		return schema("object", null, "properties", properties, "required", List.of("task"));
	}
}
