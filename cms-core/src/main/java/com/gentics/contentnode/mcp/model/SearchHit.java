package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A search hit, as returned by MCP tools: a reference plus the indexed metadata needed to choose it, never the
 * content. Fields that are not set are omitted on serialization.
 * @param ref reference to the object
 * @param score relevance score
 * @param snippets highlight fragments
 * @param language language code (pages and forms)
 * @param online whether the object is online in at least one node
 * @param edited last edit timestamp
 * @param templateId template ID (pages)
 * @param folderId folder ID
 */
@JsonInclude(Include.NON_NULL)
public record SearchHit(ObjectRef ref, Double score, List<String> snippets, String language, Boolean online,
		Integer edited, Integer templateId, Integer folderId) {
	/**
	 * Build the output schema of a hit. Must be kept in sync with the components. Only {@code ref} is required.
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema(null));
		properties.put("score", schema("number", null));
		properties.put("snippets", schema("array", "Highlight fragments.", "items", schema("string", null)));
		properties.put("language", schema("string", null));
		properties.put("online", schema("boolean", "Whether the object is online in at least one node."));
		properties.put("edited", schema("integer", "Unix timestamp in seconds."));
		properties.put("templateId", schema("integer", null));
		properties.put("folderId", schema("integer", null));
		return schema("object", "A reference plus enough metadata to choose. Call get_page for content.",
				"properties", properties, "required", List.of("ref"));
	}
}
