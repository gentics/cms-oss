package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.Page;

/**
 * Shared reference to a CMS object, used by MCP tools in both directions: emitted as part of a
 * tool's output, and accepted as a tool's input.
 *
 * <p>
 * On input, only {@link #type()} and {@link #id()} are read. A ref emitted by one tool must be
 * accepted by another tool unchanged, so every other field is optional and unknown fields are
 * ignored on deserialization. Fields that are not set are omitted on serialization.
 * </p>
 *
 * @param type type of the referenced object
 * @param id local ID of the referenced object
 * @param globalId cross-installation stable ID, where the CMS has one
 * @param nodeId ID of the node the object belongs to; set for every object below a node, never
 *        for a node itself
 * @param name name of the object
 * @param path folder path, e.g. {@code /News/2026/}
 * @param language language code (pages and forms only)
 * @param niceUrl nice URL of the object
 * @param url preview/published URL of the object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record ObjectRef(
		Type type,
		int id,
		String globalId,
		Integer nodeId,
		String name,
		String path,
		String language,
		String niceUrl,
		String url) {

	/**
	 * Type of the referenced object, serialized as its lowercase {@link #value()}.
	 */
	public enum Type {
		NODE("node"), FOLDER("folder"), PAGE("page"), FILE("file"), IMAGE("image"), FORM("form"),
		TEMPLATE("template"), CONSTRUCT("construct"), TAG("tag"), USER("user"), GROUP("group");

		private final String value;

		Type(String value) {
			this.value = value;
		}

		/**
		 * Get the wire value of this type
		 * @return lowercase wire value
		 */
		@JsonValue
		public String value() {
			return value;
		}

		/**
		 * Get the type for the given wire value (case-insensitive)
		 * @param value wire value
		 * @return type
		 * @throws IllegalArgumentException if the value does not match any type
		 */
		@JsonCreator
		public static Type fromValue(String value) {
			for (Type type : values()) {
				if (type.value.equalsIgnoreCase(value)) {
					return type;
				}
			}
			throw new IllegalArgumentException("Unknown object type '%s', expected one of: %s".formatted(value,
					Arrays.stream(values()).map(Type::value).collect(Collectors.joining(", "))));
		}
	}

	/**
	 * Create a ref with only type and ID set (the input-side shape)
	 * @param type object type
	 * @param id object ID
	 * @return ref
	 */
	public static ObjectRef of(Type type, int id) {
		return new ObjectRef(type, id, null, null, null, null, null, null, null);
	}

	/**
	 * Create the ref for a node itself (not for an object below a node, so {@link #nodeId()} is
	 * not set)
	 * @param restNode REST model of the node
	 * @return ref
	 */
	public static ObjectRef forNode(Node restNode) {
		return new ObjectRef(Type.NODE, restNode.getId(), restNode.getGlobalId(), null, restNode.getName(), null,
				null, null, null);
	}

	/**
	 * Create the ref for a page. {@link #path()} is the page's folder path
	 * ({@link Page#getPath()}, as built by {@code ModelBuilder#getFolderPath}), {@link #url()} is
	 * the page's preview URL if set, otherwise its live URL.
	 * @param restPage REST model of the page
	 * @param nodeId ID of the node the page belongs to, may be null if unknown
	 * @return ref
	 */
	public static ObjectRef forPage(Page restPage, Integer nodeId) {
		String url = restPage.getUrl() != null && !restPage.getUrl().isBlank() ? restPage.getUrl()
				: restPage.getLiveUrl();
		return new ObjectRef(Type.PAGE, restPage.getId(), restPage.getGlobalId(), nodeId, restPage.getName(),
				restPage.getPath(), restPage.getLanguage(), restPage.getNiceUrl(), url);
	}

	/**
	 * Create the ref for a page, like {@link #forPage(Page, Integer)}, taking {@link #nodeId()}
	 * from the page's folder ({@link Page#getFolder()}), which is only set if the page was loaded
	 * with its folder
	 * @param restPage REST model of the page
	 * @return ref, without node ID if the folder was not loaded
	 */
	public static ObjectRef forPage(Page restPage) {
		return forPage(restPage, restPage.getFolder() != null ? restPage.getFolder().getNodeId() : null);
	}

	/**
	 * Build the output schema of a ref. Describes its fields, but does not validate them further
	 * (nothing is required). Must be kept in sync with the components.
	 * @param description description, may be null
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema(String description) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("type", schema("string", null));
		properties.put("id", schema("integer", null));
		properties.put("globalId", schema("string", null));
		properties.put("nodeId", schema("integer", null));
		properties.put("name", schema("string", null));
		properties.put("path", schema("string", null));
		properties.put("language", schema("string", null));
		properties.put("niceUrl", schema("string", null));
		properties.put("url", schema("string", null));
		return schema("object", description, "properties", properties);
	}
}
