package com.gentics.contentnode.mcp.model;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.SelectOption;
import com.gentics.contentnode.rest.model.Tag;

/**
 * A tag of a page or template, as returned by MCP tools, with its properties flattened to plain values per part
 * keyword. Fields that are not set are omitted on serialization.
 * @param name tag name
 * @param constructId construct ID
 * @param constructKeyword construct keyword
 * @param type tag type
 * @param active whether the tag is active
 * @param properties flattened values by part keyword, see {@link #value(Property)}
 * @param propertiesRaw REST properties by part keyword, only on request
 */
@JsonInclude(Include.NON_NULL)
public record TagInfo(String name, Integer constructId, String constructKeyword, Tag.Type type, Boolean active,
		Map<String, Object> properties, Map<String, Property> propertiesRaw) {
	/**
	 * Map the REST tag
	 * @param tag REST tag
	 * @param keywords construct keywords by construct ID, see {@link #constructKeywords(Collection)}
	 * @param raw whether to add the REST properties as {@link #propertiesRaw()}
	 * @return tag info
	 */
	public static TagInfo of(Tag tag, Map<Integer, String> keywords, boolean raw) {
		Map<String, Object> properties = new LinkedHashMap<>();
		if (tag.getProperties() != null) {
			for (Map.Entry<String, Property> entry : tag.getProperties().entrySet()) {
				Object value = value(entry.getValue());
				if (value != null) {
					properties.put(entry.getKey(), value);
				}
			}
		}
		String keyword = tag.getConstruct() != null && tag.getConstruct().getKeyword() != null
				? tag.getConstruct().getKeyword()
				: keywords.get(tag.getConstructId());
		return new TagInfo(tag.getName(), tag.getConstructId(), keyword, tag.getType(), tag.getActive(), properties,
				raw ? tag.getProperties() : null);
	}

	/**
	 * Get the plain value of a property by its type: text as string, boolean, page/file/image/folder/form/node URLs as
	 * {@link ObjectRef} (an external page URL as string), lists as string list, a select as the selected value, a
	 * multiselect as list of selected values, a tag link as map of its IDs. Other types (e.g. overviews) have no plain
	 * value.
	 * @param property REST property
	 * @return value or null
	 */
	public static Object value(Property property) {
		if (property == null || property.getType() == null) {
			return null;
		}
		switch (property.getType()) {
		case STRING:
		case RICHTEXT:
			return property.getStringValue();
		case BOOLEAN:
			return property.getBooleanValue();
		case PAGE:
			return isSet(property.getPageId()) ? ObjectRef.of(Type.PAGE, property.getPageId())
					: property.getStringValue();
		case FILE:
			return ref(Type.FILE, property.getFileId());
		case IMAGE:
			return ref(Type.IMAGE, property.getImageId());
		case FOLDER:
			return ref(Type.FOLDER, property.getFolderId());
		case FORM:
		case CMSFORM:
			return ref(Type.FORM, property.getFormId());
		case NODE:
			return ref(Type.NODE, property.getNodeId());
		case LIST:
		case ORDEREDLIST:
		case UNORDEREDLIST:
			return property.getStringValues();
		case SELECT:
			return property.getSelectedOptions() == null || property.getSelectedOptions().isEmpty() ? null
					: property.getSelectedOptions().get(0).getValue();
		case MULTISELECT:
			return property.getSelectedOptions() == null ? null
					: property.getSelectedOptions().stream().map(SelectOption::getValue).toList();
		case PAGETAG:
		case TEMPLATETAG:
			Map<String, Integer> link = new LinkedHashMap<>();
			putIfSet(link, "pageId", property.getPageId());
			putIfSet(link, "contentTagId", property.getContentTagId());
			putIfSet(link, "templateId", property.getTemplateId());
			putIfSet(link, "templateTagId", property.getTemplateTagId());
			return link.isEmpty() ? null : link;
		default:
			return null;
		}
	}

	/**
	 * Load the keywords of the constructs of the given tags, in one transaction. The REST tags of a page do not carry
	 * them.
	 * @param tags REST tags
	 * @return keywords by construct ID
	 * @throws NodeException
	 */
	public static Map<Integer, String> constructKeywords(Collection<Tag> tags) throws NodeException {
		Set<Integer> ids = new HashSet<>();
		for (Tag tag : tags) {
			if (tag.getConstructId() != null) {
				ids.add(tag.getConstructId());
			}
		}
		Map<Integer, String> keywords = new HashMap<>();
		if (ids.isEmpty()) {
			return keywords;
		}
		try (Trx trx = ContentNodeHelper.trx()) {
			for (Construct construct : trx.getTransaction().getObjects(Construct.class, ids)) {
				keywords.put(construct.getId(), construct.getKeyword());
			}
			trx.success();
		}
		return keywords;
	}

	/**
	 * Build the output schema of a tag. Must be kept in sync with the components.
	 * @return schema
	 */
	public static Map<String, Object> jsonSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("name", schema("string", null));
		properties.put("constructId", schema("integer", null));
		properties.put("constructKeyword", schema("string", null));
		properties.put("type", schema("string", null, "enum", List.of("CONTENTTAG", "TEMPLATETAG", "OBJECTTAG")));
		properties.put("active", schema("boolean", null));
		properties.put("properties", schema("object", "Plain values by part keyword, typed by part type.",
				"additionalProperties", true));
		properties.put("propertiesRaw", schema("object", "Raw CMS properties by part keyword, on request only."));
		return schema("object", null, "properties", properties);
	}

	/**
	 * Get the ref for a set ID
	 * @param type object type
	 * @param id ID, may be null or 0
	 * @return ref or null
	 */
	private static ObjectRef ref(Type type, Integer id) {
		return isSet(id) ? ObjectRef.of(type, id) : null;
	}

	/**
	 * Check whether an ID is set
	 * @param id ID
	 * @return true for a positive ID
	 */
	private static boolean isSet(Integer id) {
		return id != null && id > 0;
	}

	/**
	 * Put a set ID into the map
	 * @param map map
	 * @param key key
	 * @param id ID
	 */
	private static void putIfSet(Map<String, Integer> map, String key, Integer id) {
		if (isSet(id)) {
			map.put(key, id);
		}
	}
}
