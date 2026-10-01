package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.rest.model.Node;

/**
 * A node (site or channel), as returned by MCP tools. Fields that are not set are omitted on
 * serialization.
 * @param ref reference to the node
 * @param host host of the node, as reported by the REST model (including the protocol, see
 *        {@code ModelBuilder#getNode})
 * @param publishDir publish directory of the node
 * @param languages languages assigned to the node
 * @param defaultFileFolderId ID of the default folder for files
 * @param defaultImageFolderId ID of the default folder for images
 * @param contentRepositoryId ID of the assigned content repository
 */
@JsonInclude(Include.NON_NULL)
public record NodeInfo(ObjectRef ref, String host, String publishDir, List<LanguageInfo> languages,
		Integer defaultFileFolderId, Integer defaultImageFolderId, Integer contentRepositoryId) {
	/**
	 * Map the REST node. The REST node does not contain its languages, they are loaded separately
	 * (e.g. with {@code NodeResourceImpl#languages}).
	 * @param node REST node
	 * @param languages languages assigned to the node
	 * @return node info
	 */
	public static NodeInfo of(Node node, List<LanguageInfo> languages) {
		return new NodeInfo(ObjectRef.forNode(node), node.getHost(), node.getPublishDir(), languages,
				node.getDefaultFileFolderId(), node.getDefaultImageFolderId(), node.getContentRepositoryId());
	}

	/**
	 * Build the output schema of a node info. Must be kept in sync with the components.
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("ref", ObjectRef.jsonSchema("Reference to the node."));
		properties.put("host", schema("string", null));
		properties.put("publishDir", schema("string", null));
		properties.put("languages", schema("array", "Languages assigned to the node.", "items",
				LanguageInfo.jsonSchema()));
		properties.put("defaultFileFolderId", schema("integer", null));
		properties.put("defaultImageFolderId", schema("integer", null));
		properties.put("contentRepositoryId", schema("integer", null));
		return schema("object", null, "properties", properties);
	}
}
