package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.rest.model.ContentLanguage;

/**
 * A content language (e.g. assigned to a node), as returned by MCP tools. Fields that are not set
 * are omitted on serialization.
 * @param id language ID
 * @param code language code
 * @param name language name
 */
@JsonInclude(Include.NON_NULL)
public record LanguageInfo(Integer id, String code, String name) {
	/**
	 * Map the REST language
	 * @param language REST language, may be null
	 * @return language info or null
	 */
	public static LanguageInfo of(ContentLanguage language) {
		return language != null ? new LanguageInfo(language.getId(), language.getCode(), language.getName()) : null;
	}

	/**
	 * Build the output schema of a language. Must be kept in sync with the components.
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("id", schema("integer", null));
		properties.put("code", schema("string", null));
		properties.put("name", schema("string", null));
		return schema("object", null, "properties", properties);
	}
}
