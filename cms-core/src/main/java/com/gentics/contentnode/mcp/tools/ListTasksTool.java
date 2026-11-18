package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpArgs.intArg;
import static com.gentics.contentnode.mcp.McpArgs.stringArg;
import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Tasks;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.model.task.UserTaskListResponse;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.resource.TaskResource;
import com.gentics.contentnode.rest.resource.impl.TaskResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.TaskFilterParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the tasks of the calling user. Delegates to {@link TaskResourceImpl#list}, which only returns what the
 * caller may see.
 */
public class ListTasksTool extends AbstractMcpTool {
	static final String NAME = "list_tasks";

	static final String ARG_STATUS = "status";

	static final String ARG_SOURCE = "source";

	static final String ARG_TYPE = "type";

	static final String ARG_SESSION_ID = "genaixSessionId";

	static final String ARG_PAGE = "page";

	static final String ARG_PAGE_SIZE = "pageSize";

	static final int MAX_PAGE_SIZE = 100;

	static final List<String> ALL_SOURCES = Arrays.stream(TaskSource.values()).map(Enum::name).toList();

	/**
	 * Result of the tool
	 * @param items tasks of the requested page
	 * @param total number of matching tasks
	 * @param hasMore whether more pages follow
	 */
	public record Result(List<UserTaskModel> items, int total, boolean hasMore) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_STATUS, schema("array", "Statuses to list. open and inProgress if omitted.", "items",
				schema("string", null, "enum", Tasks.STATUSES), "maxItems", Tasks.STATUSES.size(), "uniqueItems",
				true));
		properties.put(ARG_SOURCE, schema("array", "Sources to list. All if omitted.", "items",
				schema("string", null, "enum", ALL_SOURCES), "maxItems", ALL_SOURCES.size(), "uniqueItems", true));
		properties.put(ARG_TYPE, schema("string", "Only tasks of this type.", "minLength", 1, "maxLength", 255));
		properties.put(ARG_SESSION_ID, schema("string", "Only tasks linked to this GenAIx session.", "minLength", 1,
				"maxLength", 255));
		properties.put(Tasks.ARG_EXTERNAL_KEY, schema("string", "Only the task with this external key.",
				"minLength", 1, "maxLength", 255));
		properties.put(ARG_PAGE, schema("integer", "Page to return, from 1.", "minimum", 1, "default", 1));
		properties.put(ARG_PAGE_SIZE, schema("integer", "Tasks per page.", "minimum", 1, "maximum", MAX_PAGE_SIZE,
				"default", MAX_PAGE_SIZE));

		Map<String, Object> output = new LinkedHashMap<>();
		output.put("items", schema("array", null, "items", Tasks.taskSchema(null)));
		output.put("total", schema("integer", "Number of matching tasks."));
		output.put("hasMore", schema("boolean", "Whether more pages follow."));

		return Tool.builder().name(NAME).title("List tasks")
				.description("Lists your tasks: those assigned to you or your groups, and those you created. "
						+ "Includes the tasks the CMS derives itself (publish queue, link checker) unless you "
						+ "filter by genaixSessionId or externalKey.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", output, "required", List.of("items", "total")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		TaskFilterParameterBean filter = new TaskFilterParameterBean();
		List<String> status = Args.enumList(arguments, ARG_STATUS, Tasks.STATUSES, null);
		if (status != null && !status.isEmpty()) {
			Set<TaskStatus> wanted = new LinkedHashSet<>();
			status.forEach(name -> wanted.add(TaskStatus.valueOf(name)));
			filter.status = wanted;
		}
		List<String> source = Args.enumList(arguments, ARG_SOURCE, ALL_SOURCES, null);
		if (source != null && !source.isEmpty()) {
			Set<TaskSource> wanted = new LinkedHashSet<>();
			source.forEach(name -> wanted.add(TaskSource.valueOf(name)));
			filter.source = wanted;
		}
		filter.type = stringArg(arguments, ARG_TYPE, 1, 255);
		filter.genaixSessionId = stringArg(arguments, ARG_SESSION_ID, 1, 255);
		filter.externalKey = stringArg(arguments, Tasks.ARG_EXTERNAL_KEY, 1, 255);
		PagingParameterBean paging = new PagingParameterBean()
				.setPage(intArg(arguments, ARG_PAGE, 1, Integer.MAX_VALUE, 1))
				.setPageSize(intArg(arguments, ARG_PAGE_SIZE, 1, MAX_PAGE_SIZE, MAX_PAGE_SIZE));

		TaskResource resource = RestPermissions.guard(TaskResource.class, new TaskResourceImpl());
		UserTaskListResponse response = resource.list(null, null, paging, filter);
		requireOk(response, "The tasks could not be listed");
		return new Result(response.getItems(), response.getNumItems(), response.getHasMoreItems());
	}
}
