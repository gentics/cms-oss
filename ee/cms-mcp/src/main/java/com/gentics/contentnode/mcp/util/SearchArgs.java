package com.gentics.contentnode.mcp.util;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.contentnode.mcp.McpArgs;

/**
 * Scope and filter arguments shared by the search tools
 * @param types object types
 * @param nodeId node ID, may be null
 * @param folderIds folder IDs, may be null
 * @param recursive whether to include the subfolders of the folders
 * @param languages language codes, may be null
 * @param online online filter, may be null
 * @param templateIds template filter, may be null
 * @param editedAfter edit timestamp filter, may be null
 */
public record SearchArgs(List<String> types, Integer nodeId, List<Integer> folderIds, boolean recursive,
		List<String> languages, Boolean online, List<Integer> templateIds, Integer editedAfter) {
	public static final String ARG_TYPES = "types";

	public static final String ARG_NODE_ID = "nodeId";

	public static final String ARG_FOLDER_ID = "folderId";

	public static final String ARG_RECURSIVE = "recursive";

	public static final String ARG_LANGUAGES = "languages";

	public static final String ARG_FILTERS = "filters";

	/**
	 * Object types of the search indices
	 */
	public static final List<String> TYPES = List.of("page", "folder", "file", "image", "form");

	/**
	 * Parse and validate the arguments
	 * @param arguments arguments
	 * @return search arguments
	 * @throws IllegalArgumentException if an argument is invalid
	 */
	@SuppressWarnings("unchecked")
	public static SearchArgs of(Map<String, Object> arguments) {
		List<String> types = Args.enumList(arguments, ARG_TYPES, TYPES, List.of("page"));
		if (types.isEmpty()) {
			throw new IllegalArgumentException("Argument '%s' must not be empty".formatted(ARG_TYPES));
		}
		Map<String, Object> filters = Map.of();
		Object rawFilters = arguments.get(ARG_FILTERS);
		if (rawFilters instanceof Map<?, ?> map) {
			filters = (Map<String, Object>) map;
		} else if (rawFilters != null) {
			throw new IllegalArgumentException("Argument '%s' must be an object".formatted(ARG_FILTERS));
		}
		Boolean online = filters.containsKey("online") ? McpArgs.booleanArg(filters, "online", false) : null;
		return new SearchArgs(types, McpArgs.intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE),
				Args.intList(arguments, ARG_FOLDER_ID, 20), McpArgs.booleanArg(arguments, ARG_RECURSIVE, true),
				Args.stringList(arguments, ARG_LANGUAGES, 20, 10), online, Args.intList(filters, "templateIds", 50),
				McpArgs.intArg(filters, "editedAfter", 0, Integer.MAX_VALUE));
	}

	/**
	 * Build the schemas of the arguments
	 * @param folders whether to add folderId and recursive
	 * @param filters whether to add filters
	 * @return argument schemas
	 */
	public static Map<String, Object> schemaProperties(boolean folders, boolean filters) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_TYPES, schema("array", "Object types to search.", "minItems", 1, "maxItems", 5,
				"uniqueItems", true, "items", schema("string", null, "enum", TYPES), "default", List.of("page")));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to search in.", "minimum", 1));
		if (folders) {
			properties.put(ARG_FOLDER_ID, schema("array", "Folders to search in.", "maxItems", 20, "items",
					schema("integer", null, "minimum", 1)));
			properties.put(ARG_RECURSIVE, schema("boolean",
					"Include subfolders of folderId. Ignored when folderId is absent.", "default", true));
		}
		properties.put(ARG_LANGUAGES, schema("array", "Language codes. Narrows which per-language indices are searched.",
				"maxItems", 20, "uniqueItems", true, "items", schema("string", null, "maxLength", 10)));
		if (filters) {
			Map<String, Object> filterProperties = new LinkedHashMap<>();
			filterProperties.put("online", schema("boolean",
					"True for published objects only, false for offline only. Omit for both."));
			filterProperties.put("templateIds", schema("array", "Restrict to pages built on these templates.",
					"maxItems", 50, "items", schema("integer", null, "minimum", 1)));
			filterProperties.put("editedAfter", schema("integer",
					"Only objects edited at or after this Unix timestamp."));
			properties.put(ARG_FILTERS, schema("object", "Filters, AND-combined.", "additionalProperties", false,
					"properties", filterProperties));
		}
		return properties;
	}

	/**
	 * Get the filter clauses of the filters
	 * @return filter clauses
	 */
	public List<JsonNode> filterClauses() {
		return SearchBodies.filters(online, templateIds, editedAfter);
	}

	/**
	 * Get the scope of the search
	 * @return scope
	 */
	public EnterpriseSearch.Scope scope() {
		return new EnterpriseSearch.Scope(types, nodeId, folderIds, recursive, languages);
	}
}
