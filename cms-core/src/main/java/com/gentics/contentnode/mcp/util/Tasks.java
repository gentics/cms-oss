package com.gentics.contentnode.mcp.util;

import static com.gentics.contentnode.mcp.McpArgs.stringArg;
import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.model.task.UserTaskModel;
import com.gentics.contentnode.rest.resource.TaskResource;
import com.gentics.contentnode.rest.resource.parameter.TaskFilterParameterBean;

/**
 * Shared by the task tools: the arguments that make up a task, how a stored task is addressed (by its ID, or by
 * its external key) and the output schema of a task.
 */
public final class Tasks {
	public static final String ARG_ID = "id";

	public static final String ARG_EXTERNAL_KEY = "externalKey";

	public static final String ARG_SOURCE = "source";

	/**
	 * Sources a stored task can have. Tasks of the other sources are virtual: the CMS derives them.
	 */
	public static final List<String> SOURCES = Arrays.stream(TaskSource.values()).filter(s -> !s.isVirtual())
			.map(Enum::name).toList();

	public static final List<String> STATUSES = Arrays.stream(TaskStatus.values()).map(Enum::name).toList();

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

	private Tasks() {
	}

	/**
	 * Input properties addressing a stored task
	 * @return properties
	 */
	public static Map<String, Object> addressProperties() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, schema("string", "ID of the task. Pass either this or externalKey.", "minLength", 1,
				"maxLength", 64));
		properties.put(ARG_EXTERNAL_KEY, schema("string", "External key of the task. Pass either this or id.",
				"minLength", 1, "maxLength", 255));
		properties.put(ARG_SOURCE, schema("string", "Source the external key belongs to. Any source if omitted.",
				"enum", SOURCES));
		return properties;
	}

	/**
	 * Input properties that make up a task: what {@code POST /rest/task} and {@code PUT /rest/task/{id}} accept
	 * @return properties
	 */
	public static Map<String, Object> taskProperties() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("type", schema("string", "Type of the task, e.g. genaixReadyToPublish.", "minLength", 1,
				"maxLength", 255));
		properties.put("title", schema("string", "Title shown in the task list.", "minLength", 1, "maxLength", 255));
		properties.put("description", schema("string", "What is to be done.", "maxLength", 10000));
		properties.put("status", schema("string", "Status of the task.", "enum", STATUSES));
		properties.put("dueAt", schema("integer", "Due date as unix timestamp.", "minimum", 0));

		Map<String, Object> assignee = new LinkedHashMap<>();
		assignee.put("type", schema("string", null, "enum", List.of("user", "group")));
		assignee.put("id", schema("integer", null, "minimum", 1));
		properties.put("assignee", schema("object", "Who gets the task: a user or a group. You, if omitted. The "
				+ "assignee must be allowed to view all objects of the task.", "properties", assignee, "required",
				List.of("type", "id"), "additionalProperties", false));

		Map<String, Object> object = new LinkedHashMap<>();
		object.put("type", schema("string", "Object type, e.g. page, folder, file, image."));
		object.put("id", schema("integer", null, "minimum", 1));
		object.put("nodeId", schema("integer", null, "minimum", 1));
		object.put("name", schema("string", null));
		properties.put("objects", schema("array", "CMS objects the task is about.", "maxItems", 100, "items",
				schema("object", null, "properties", object, "required", List.of("type", "id"),
						"additionalProperties", false)));

		Map<String, Object> genaix = new LinkedHashMap<>();
		genaix.put("sessionId", schema("string", "GenAIx session the task belongs to."));
		genaix.put("planId", schema("string", "Plan of that session."));
		genaix.put("instruction", schema("string", "Instruction for the agent of whoever starts a session from "
				+ "the task."));
		properties.put("genaix", schema("object", "Link to GenAIx.", "properties", genaix, "additionalProperties",
				false));
		return properties;
	}

	/**
	 * Output schema of a task
	 * @param description description
	 * @return schema
	 */
	public static Map<String, Object> taskSchema(String description) {
		return schema("object", description);
	}

	/**
	 * Build the task model from the arguments
	 * @param arguments arguments
	 * @param ignored names of arguments that are not members of the task
	 * @return task model
	 * @throws IllegalArgumentException if the arguments do not make up a task
	 */
	public static UserTaskModel model(Map<String, Object> arguments, String... ignored) {
		Map<String, Object> members = new LinkedHashMap<>(arguments);
		members.keySet().removeAll(Set.of(ignored));
		try {
			return MAPPER.convertValue(members, UserTaskModel.class);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("The arguments do not make up a task: " + e.getMessage(), e);
		}
	}

	/**
	 * Resolve the ID of the stored task the arguments address, among the tasks the calling user can see
	 * @param resource task resource
	 * @param arguments arguments
	 * @return ID of the task, null if no task has the given external key
	 * @throws Exception on errors
	 * @throws IllegalArgumentException unless exactly one of id and externalKey is passed
	 */
	public static String resolveId(TaskResource resource, Map<String, Object> arguments) throws Exception {
		if (ARG_ID.equals(Args.exactlyOne(arguments, ARG_ID, ARG_EXTERNAL_KEY))) {
			return stringArg(arguments, ARG_ID, 1, 64);
		}
		TaskFilterParameterBean filter = new TaskFilterParameterBean();
		filter.externalKey = stringArg(arguments, ARG_EXTERNAL_KEY, 1, 255);
		filter.status = EnumSet.allOf(TaskStatus.class);
		String source = stringArg(arguments, ARG_SOURCE, 1, 32);
		if (source != null) {
			filter.source = Set.of(source(source));
		}
		List<UserTaskModel> found = resource.list(null, null, null, filter).getItems();
		return found == null || found.isEmpty() ? null : found.get(0).getId();
	}

	/**
	 * Parse the source of a stored task
	 * @param name name of the source
	 * @return source
	 * @throws IllegalArgumentException if no stored task can have that source
	 */
	public static TaskSource source(String name) {
		if (!SOURCES.contains(name)) {
			throw new IllegalArgumentException("Argument '%s' must be one of %s".formatted(ARG_SOURCE, SOURCES));
		}
		return TaskSource.valueOf(name);
	}
}
