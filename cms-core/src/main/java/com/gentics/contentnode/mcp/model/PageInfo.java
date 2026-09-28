package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.rest.model.Page;

/**
 * Metadata of a page, as returned by MCP tools. Has no tags: the page content is left to tools
 * that are explicitly about content. Timestamps are unix timestamps; timestamps that are not set
 * (e.g. {@code published} for a page never published, {@code lockedSince} for an unlocked page)
 * are omitted, as are all other fields that are not set.
 * @param ref reference to the page
 * @param fileName filename
 * @param description description
 * @param niceUrl nice URL
 * @param templateId template ID
 * @param folderId folder ID
 * @param language language code
 * @param languageName language name
 * @param priority priority
 * @param url preview URL
 * @param liveUrl live URL
 * @param publishPath publish path
 * @param online whether the page is online
 * @param modified whether the page is modified since it was last published
 * @param queued whether the page is queued for publishing
 * @param planned whether the page has planned time management
 * @param locked whether the page is locked
 * @param lockedBy user who locked the page
 * @param lockedSince timestamp since when the page is locked
 * @param creator creator
 * @param created creation timestamp
 * @param editor last editor
 * @param edited last edit timestamp
 * @param publisher last publisher
 * @param published last publish timestamp
 * @param versions page versions, newest first
 * @param currentVersion current version
 * @param publishedVersion published version
 * @param languageVariants language variants by language code
 * @param translationStatus translation status
 */
@JsonInclude(Include.NON_NULL)
public record PageInfo(ObjectRef ref, String fileName, String description, String niceUrl, Integer templateId,
		Integer folderId, String language, String languageName, Integer priority, String url, String liveUrl,
		String publishPath, boolean online, boolean modified, boolean queued, boolean planned, boolean locked,
		UserRef lockedBy, Integer lockedSince, UserRef creator, Integer created, UserRef editor, Integer edited,
		UserRef publisher, Integer published, List<VersionInfo> versions, VersionInfo currentVersion,
		VersionInfo publishedVersion, Map<String, ObjectRef> languageVariants,
		TranslationStatusInfo translationStatus) {

	/**
	 * Map the REST page. Fields the page was not loaded with (e.g. versions, language variants,
	 * translation status, or the folder, from which {@link ObjectRef#nodeId()} is taken) are
	 * omitted.
	 * @param page REST page
	 * @return page info
	 */
	public static PageInfo of(Page page) {
		List<VersionInfo> versions = page.getVersions() == null ? null
				: page.getVersions().stream().map(VersionInfo::of).toList();

		Map<String, ObjectRef> languageVariants = null;
		if (page.getLanguageVariants() != null) {
			languageVariants = new LinkedHashMap<>();
			for (Map.Entry<Object, Page> entry : page.getLanguageVariants().entrySet()) {
				Page variant = entry.getValue();
				String key = variant.getLanguage() != null ? variant.getLanguage() : String.valueOf(entry.getKey());
				languageVariants.put(key, ObjectRef.forPage(variant));
			}
		}

		return new PageInfo(ObjectRef.forPage(page), page.getFileName(), page.getDescription(), page.getNiceUrl(),
				page.getTemplateId(), page.getFolderId(), page.getLanguage(), page.getLanguageName(),
				page.getPriority(), page.getUrl(), page.getLiveUrl(), page.getPublishPath(), page.isOnline(),
				page.isModified(), page.isQueued(), page.isPlanned(), page.isLocked(), UserRef.of(page.getLockedBy()),
				Timestamps.orNull(page.getLockedSince()), UserRef.of(page.getCreator()),
				Timestamps.orNull(page.getCdate()), UserRef.of(page.getEditor()), Timestamps.orNull(page.getEdate()),
				UserRef.of(page.getPublisher()), Timestamps.orNull(page.getPdate()), versions,
				VersionInfo.of(page.getCurrentVersion()), VersionInfo.of(page.getPublishedVersion()),
				languageVariants, TranslationStatusInfo.of(page.getTranslationStatus()));
	}

	/**
	 * Build the output schema of a page info. Must be kept in sync with the components. Only
	 * {@code ref} is required.
	 * @param description description, may be null
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema(String description) {
		Map<String, Object> refSchema = ObjectRef.jsonSchema(null);
		Map<String, Object> userSchema = UserRef.jsonSchema();
		Map<String, Object> versionSchema = VersionInfo.jsonSchema();

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", refSchema);
		properties.put("fileName", schema("string", null));
		properties.put("description", schema("string", null));
		properties.put("niceUrl", schema("string", null));
		properties.put("templateId", schema("integer", null));
		properties.put("folderId", schema("integer", null));
		properties.put("language", schema("string", null));
		properties.put("languageName", schema("string", null));
		properties.put("priority", schema("integer", null));
		properties.put("url", schema("string", null));
		properties.put("liveUrl", schema("string", null));
		properties.put("publishPath", schema("string", null));
		properties.put("online", schema("boolean", null));
		properties.put("modified", schema("boolean", null));
		properties.put("queued", schema("boolean", null));
		properties.put("planned", schema("boolean", null));
		properties.put("locked", schema("boolean", null));
		properties.put("lockedBy", userSchema);
		properties.put("lockedSince", schema("integer", null));
		properties.put("creator", userSchema);
		properties.put("created", schema("integer", null));
		properties.put("editor", userSchema);
		properties.put("edited", schema("integer", null));
		properties.put("publisher", userSchema);
		properties.put("published", schema("integer", null));
		properties.put("versions", schema("array", "Page versions, newest first.", "items", versionSchema));
		properties.put("currentVersion", versionSchema);
		properties.put("publishedVersion", versionSchema);
		properties.put("languageVariants", schema("object", "Language variants by language code.",
				"additionalProperties", refSchema));
		properties.put("translationStatus", TranslationStatusInfo.jsonSchema());

		return schema("object", description, "properties", properties, "required", List.of("ref"));
	}
}
