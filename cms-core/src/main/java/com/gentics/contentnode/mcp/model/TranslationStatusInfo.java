package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.rest.model.TranslationStatus;

/**
 * Translation status of a page, as returned by MCP tools. For a page that is not synchronized
 * with another language variant, only {@code inSync} (true) is set.
 * @param pageId ID of the page this page is synchronized with
 * @param name name of that page
 * @param language language code of that page
 * @param inSync whether this page is in sync with that page
 * @param version version of that page this page is synchronized with
 * @param versionTimestamp timestamp of that version
 * @param latestVersion latest version of that page
 */
@JsonInclude(Include.NON_NULL)
public record TranslationStatusInfo(Integer pageId, String name, String language, boolean inSync, String version,
		Integer versionTimestamp, LatestVersionInfo latestVersion) {
	/**
	 * Map the REST translation status
	 * @param status REST translation status, may be null
	 * @return translation status info or null
	 */
	public static TranslationStatusInfo of(TranslationStatus status) {
		if (status == null) {
			return null;
		}
		TranslationStatus.Latest latest = status.getLatestVersion();
		return new TranslationStatusInfo(status.getPageId(), status.getName(), status.getLanguage(),
				status.isInSync(), status.getVersion(), status.getVersionTimestamp(),
				latest != null ? new LatestVersionInfo(latest.getVersion(), latest.getVersionTimestamp()) : null);
	}

	/**
	 * Build the output schema of a translation status. Must be kept in sync with the components.
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("pageId", schema("integer", null));
		properties.put("name", schema("string", null));
		properties.put("language", schema("string", null));
		properties.put("inSync", schema("boolean", null));
		properties.put("version", schema("string", null));
		properties.put("versionTimestamp", schema("integer", null));
		properties.put("latestVersion", LatestVersionInfo.jsonSchema());
		return schema("object", null, "properties", properties);
	}

	/**
	 * Latest version of the page a page is synchronized with
	 * @param version version number
	 * @param versionTimestamp version timestamp
	 */
	@JsonInclude(Include.NON_NULL)
	public record LatestVersionInfo(String version, Integer versionTimestamp) {
		/**
		 * Build the output schema of the latest version. Must be kept in sync with the components.
		 * @return schema
		 */
		public static Map<String, Object> jsonSchema() {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("version", schema("string", null));
			properties.put("versionTimestamp", schema("integer", null));
			return schema("object", null, "properties", properties);
		}
	}
}
