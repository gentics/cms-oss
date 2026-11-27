package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
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
 * Changes a stored task, addressed by its ID or its external key. Delegates to {@link TaskResourceImpl#update},
 * which decides who may change what: the assignee the status, the creator everything else.
 */
public class UpdateTaskTool extends AbstractMcpTool {
	static final String NAME = "update_task";

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>(Tasks.addressProperties());
		properties.putAll(Tasks.taskProperties());
		properties.remove("type");

		return Tool.builder().name(NAME).title("Update task")
				.description("Changes a task: its status (e.g. done), title, description, due date, assignee or "
						+ "objects. Members you do not pass stay as they are. The task is addressed by its id, or "
						+ "by its externalKey. Tasks the CMS derives itself, such as the publish queue, cannot be "
						+ "changed this way.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(CreateTaskTool.outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		UserTaskModel changes = Tasks.model(arguments, Tasks.ARG_ID, Tasks.ARG_EXTERNAL_KEY, Tasks.ARG_SOURCE);

		TaskResource resource = RestPermissions.guard(TaskResource.class, new TaskResourceImpl());
		String id = Tasks.resolveId(resource, arguments);
		if (id == null) {
			throw new EntityNotFoundException("No task has the external key '%s'"
					.formatted(arguments.get(Tasks.ARG_EXTERNAL_KEY)));
		}
		UserTaskResponse response = resource.update(id, changes);
		requireOk(response, "Task %s was not saved".formatted(id));
		return new CreateTaskTool.Result(response.getTask());
	}
}
