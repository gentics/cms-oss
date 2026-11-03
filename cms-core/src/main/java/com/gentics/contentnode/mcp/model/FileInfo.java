package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Image;

/**
 * Metadata of a file or image, as returned by MCP tools. Fields that are not set (e.g. width and height of a file)
 * are omitted on serialization.
 * @param ref reference to the file or image
 * @param fileName filename
 * @param description description
 * @param sizeBytes size in bytes
 * @param mimeType mime type
 * @param width width in pixels (images only)
 * @param height height in pixels (images only)
 * @param online whether the file is online
 * @param niceUrl nice URL
 * @param url live URL, or the preview URL if there is no live URL
 * @param created creation timestamp
 * @param edited last edit timestamp
 * @param folderId folder ID
 */
@JsonInclude(Include.NON_NULL)
public record FileInfo(ObjectRef ref, String fileName, String description, Integer sizeBytes, String mimeType,
		Integer width, Integer height, boolean online, String niceUrl, String url, Integer created, Integer edited,
		Integer folderId) {
	/**
	 * Map the REST file or image. The node of the ref is the given node, or the node the file lives in.
	 * @param file REST file or image
	 * @param nodeId node ID the file was loaded for, may be null
	 * @return file info
	 */
	public static FileInfo of(File file, Integer nodeId) {
		Integer refNodeId = nodeId != null ? nodeId : file.getInheritedFromId();
		ObjectRef ref;
		Integer width = null;
		Integer height = null;
		if (file instanceof Image image) {
			ref = ObjectRef.forImage(image, refNodeId);
			width = image.getSizeX();
			height = image.getSizeY();
		} else {
			ref = ObjectRef.forFile(file, refNodeId);
		}
		String url = file.getLiveUrl() != null && !file.getLiveUrl().isBlank() ? file.getLiveUrl() : file.getUrl();
		return new FileInfo(ref, file.getName(), file.getDescription(), file.getFileSize(), file.getFileType(), width,
				height, file.isOnline(), file.getNiceUrl(), url, Timestamps.orNull(file.getCdate()),
				Timestamps.orNull(file.getEdate()), file.getFolderId());
	}

	/**
	 * Build the output schema of a file info. Must be kept in sync with the components. Only {@code ref} is required.
	 * @param description description, may be null
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema(String description) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema(null));
		properties.put("fileName", schema("string", null));
		properties.put("description", schema("string", null));
		properties.put("sizeBytes", schema("integer", null));
		properties.put("mimeType", schema("string", null));
		properties.put("width", schema("integer", "Width in pixels, images only."));
		properties.put("height", schema("integer", "Height in pixels, images only."));
		properties.put("online", schema("boolean", null));
		properties.put("niceUrl", schema("string", null));
		properties.put("url", schema("string", null));
		properties.put("created", schema("integer", "Unix timestamp in seconds."));
		properties.put("edited", schema("integer", "Unix timestamp in seconds."));
		properties.put("folderId", schema("integer", null));
		return schema("object", description, "properties", properties, "required", List.of("ref"));
	}
}
