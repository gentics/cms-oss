package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.rest.model.ItemVersion;

/**
 * A version of a versioned object (e.g. a page), as returned by MCP tools. Fields that are not
 * set are omitted on serialization.
 * @param number version number
 * @param timestamp version timestamp
 * @param editor editor of the version
 */
@JsonInclude(Include.NON_NULL)
public record VersionInfo(String number, Integer timestamp, UserRef editor) {
	/**
	 * Map the REST version
	 * @param version REST version, may be null
	 * @return version info or null
	 */
	public static VersionInfo of(ItemVersion version) {
		return version != null ? new VersionInfo(version.getNumber(), version.getTimestamp(),
				UserRef.of(version.getEditor())) : null;
	}

	/**
	 * Build the output schema of a version. Must be kept in sync with the components.
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("number", schema("string", null));
		properties.put("timestamp", schema("integer", null));
		properties.put("editor", UserRef.jsonSchema());
		return schema("object", null, "properties", properties);
	}
}
