package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Page;

/**
 * An item of a folder (page, file, image or folder), as returned by MCP tools. Fields that do not apply to the item
 * type are omitted on serialization.
 * @param ref reference to the item
 * @param online whether the item is online (pages, files, images)
 * @param modified whether the page is modified since it was last published (pages)
 * @param edited last edit timestamp
 * @param editor last editor
 * @param templateId template ID (pages)
 * @param fileName filename (pages, files, images)
 * @param mimeType mime type (files, images)
 * @param sizeBytes size in bytes (files, images)
 */
@JsonInclude(Include.NON_NULL)
public record FolderItem(ObjectRef ref, Boolean online, Boolean modified, Integer edited, UserRef editor,
		Integer templateId, String fileName, String mimeType, Integer sizeBytes) {
	/**
	 * Map a REST item
	 * @param item REST page, file, image or folder
	 * @param nodeId node ID the items were listed for, may be null
	 * @return folder item
	 * @throws IllegalArgumentException for other item types
	 */
	public static FolderItem of(ContentNodeItem item, Integer nodeId) {
		Integer edited = Timestamps.orNull(item.getEdate());
		UserRef editor = UserRef.of(item.getEditor());
		if (item instanceof Page page) {
			ObjectRef ref = ObjectRef.forPage(page, nodeId != null ? nodeId : page.getInheritedFromId());
			return new FolderItem(ref, page.isOnline(), page.isModified(), edited, editor, page.getTemplateId(),
					page.getFileName(), null, null);
		}
		if (item instanceof File file) {
			Integer refNodeId = nodeId != null ? nodeId : file.getInheritedFromId();
			ObjectRef ref = file instanceof Image image ? ObjectRef.forImage(image, refNodeId)
					: ObjectRef.forFile(file, refNodeId);
			return new FolderItem(ref, file.isOnline(), null, edited, editor, null, file.getName(), file.getFileType(),
					file.getFileSize());
		}
		if (item instanceof Folder folder) {
			return new FolderItem(ObjectRef.forFolder(folder), null, null, edited, editor, null, null, null, null);
		}
		throw new IllegalArgumentException("Unsupported item type %s".formatted(item.getClass().getSimpleName()));
	}

	/**
	 * Build the output schema of a folder item. Must be kept in sync with the components. Only {@code ref} is
	 * required.
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema(null));
		properties.put("online", schema("boolean", null));
		properties.put("modified", schema("boolean", null));
		properties.put("edited", schema("integer", "Unix timestamp in seconds."));
		properties.put("editor", UserRef.jsonSchema());
		properties.put("templateId", schema("integer", null));
		properties.put("fileName", schema("string", null));
		properties.put("mimeType", schema("string", null));
		properties.put("sizeBytes", schema("integer", null));
		return schema("object", null, "properties", properties, "required", List.of("ref"));
	}
}
