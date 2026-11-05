package com.gentics.contentnode.mcp.util;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.TagInfo;
import com.gentics.contentnode.rest.model.NodeIdObjectId;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.SelectOption;

/**
 * Writes plain part values into REST properties, by the part's type. Every value {@link TagInfo#value(Property)}
 * returns can be written back unchanged, so a tag read with get_page_tags can be changed and written with
 * update_page_tags without knowing the CMS property model. Object references are also accepted as plain IDs.
 */
public final class PartValues {
	/**
	 * Part types whose values can be written
	 */
	public static final Set<Property.Type> WRITABLE = EnumSet.of(Property.Type.STRING, Property.Type.RICHTEXT,
			Property.Type.BOOLEAN, Property.Type.PAGE, Property.Type.FILE, Property.Type.IMAGE, Property.Type.FOLDER,
			Property.Type.NODE, Property.Type.FORM, Property.Type.CMSFORM, Property.Type.LIST,
			Property.Type.ORDEREDLIST, Property.Type.UNORDEREDLIST, Property.Type.SELECT, Property.Type.MULTISELECT,
			Property.Type.DATASOURCE, Property.Type.OVERVIEW, Property.Type.PAGETAG, Property.Type.TEMPLATETAG);

	/**
	 * How the values of each part type are written, for tool descriptions
	 */
	public static final String VALUE_SHAPES = "Values by part type, the same shapes get_page_tags returns: "
			+ "STRING and RICHTEXT a string (rich text is HTML; a tag embedded in it is <node TAGNAME>); "
			+ "BOOLEAN true or false; IMAGE, FILE, FOLDER, NODE and CMSFORM the object's ID or its ref "
			+ "{type, id}; PAGE the page's ID, its ref, or an external URL as string (an empty string clears it); FORM the form's UUID; "
			+ "LIST, ORDEREDLIST and UNORDEREDLIST an array of strings; SELECT the value of one option (null "
			+ "clears it); MULTISELECT an array of option values; DATASOURCE an array of {key, value} entries; "
			+ "OVERVIEW {listType: PAGE|FOLDER|IMAGE|FILE, selectType: MANUAL|FOLDER|AUTO, orderBy: "
			+ "ALPHABETICALLY|PRIORITY|PDATE|EDATE|CDATE|FILESIZE|SELF, orderDirection: ASC|DESC, maxItems, "
			+ "recursive, items: [refs]}, members you leave out keep their value; PAGETAG {pageId, contentTagId} "
			+ "or {pageId, templateTagId}; TEMPLATETAG {templateId, templateTagId}. The option values of a "
			+ "select part come from get_datasource.";

	private PartValues() {
	}

	/**
	 * Get the JSON schema of the values update_page_tags writes into a part of the given type
	 * @param type part type
	 * @param options values of the options of a select part, may be null
	 * @return schema, null if the type cannot be written
	 */
	public static Map<String, Object> valueSchema(Property.Type type, List<String> options) {
		if (type == null || !WRITABLE.contains(type)) {
			return null;
		}
		return switch (type) {
		case STRING -> schema("string", "Plain text.");
		case RICHTEXT -> schema("string", "HTML. A tag of this page is embedded with <node TAGNAME>; keep the "
				+ "<node> elements you read when you change the text.");
		case FORM -> schema("string", "UUID of the form.");
		case BOOLEAN -> schema("boolean", null);
		case IMAGE, FILE, FOLDER, NODE, CMSFORM -> ref(type == Property.Type.CMSFORM ? "form"
				: type.name().toLowerCase());
		case PAGE -> {
			Map<String, Object> page = ref("page");
			@SuppressWarnings("unchecked")
			List<Object> anyOf = new ArrayList<>((List<Object>) page.get("anyOf"));
			anyOf.add(schema("string", "External URL, an empty string clears the part."));
			page.put("anyOf", anyOf);
			yield page;
		}
		case LIST, ORDEREDLIST, UNORDEREDLIST -> schema("array", "One string per list item.", "items",
				schema("string", null));
		case SELECT -> options != null
				? schema("string", "Value of one option, null clears the selection.", "enum", options)
				: schema("string", "Value of one option (see get_datasource), null clears the selection.");
		case MULTISELECT -> schema("array", "Values of the selected options.", "items",
				options != null ? schema("string", null, "enum", options) : schema("string", null),
				"uniqueItems", true);
		case DATASOURCE -> schema("array", "The entries, in order.", "items", schema("object", null, "properties",
				Map.of("key", schema("string", null), "value", schema("string", null)), "required",
				List.of("key", "value"), "additionalProperties", false));
		case OVERVIEW -> {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("listType", schema("string", null, "enum", List.of("PAGE", "FOLDER", "IMAGE", "FILE")));
			properties.put("selectType", schema("string",
					"MANUAL: the items; FOLDER: the items are folders whose objects are listed; AUTO: the folder "
							+ "of the page.",
					"enum", List.of("MANUAL", "FOLDER", "AUTO")));
			properties.put("orderBy", schema("string", null, "enum",
					List.of("ALPHABETICALLY", "PRIORITY", "PDATE", "EDATE", "CDATE", "FILESIZE", "SELF")));
			properties.put("orderDirection", schema("string", null, "enum", List.of("ASC", "DESC")));
			properties.put("maxItems", schema("integer", "0 for no limit.", "minimum", 0));
			properties.put("recursive", schema("boolean", null));
			properties.put("items", schema("array", "Selected objects, IDs or refs {type, id, nodeId}.", "items",
					Map.of("anyOf", List.of(schema("integer", null, "minimum", 1), schema("object", null)))));
			yield schema("object", "Members you leave out keep their value.", "properties", properties,
					"additionalProperties", false);
		}
		case PAGETAG -> schema("object", "A tag of a page: contentTagId or templateTagId.", "properties",
				Map.of("pageId", schema("integer", null, "minimum", 1), "contentTagId",
						schema("integer", null, "minimum", 1), "templateTagId", schema("integer", null, "minimum", 1)),
				"required", List.of("pageId"), "additionalProperties", false);
		case TEMPLATETAG -> schema("object", "A tag of a template.", "properties",
				Map.of("templateId", schema("integer", null, "minimum", 1), "templateTagId",
						schema("integer", null, "minimum", 1)),
				"required", List.of("templateId", "templateTagId"), "additionalProperties", false);
		default -> null;
		};
	}

	/**
	 * Get the schema of an object reference, as ID or as ref
	 * @param type ref type
	 * @return schema
	 */
	private static Map<String, Object> ref(String type) {
		Map<String, Object> ref = new LinkedHashMap<>();
		ref.put("description", "ID of the %s, or its ref {type: %s, id}.".formatted(type, type));
		ref.put("anyOf", List.of(schema("integer", null, "minimum", 1),
				schema("object", null, "properties", Map.of("type", schema("string", null, "const", type), "id",
						schema("integer", null, "minimum", 1)), "required", List.of("id"))));
		return ref;
	}

	/**
	 * Set a value into a property, by the property's type. A select's options must be on the property.
	 * @param tag tag name, for messages
	 * @param part part keyword, for messages
	 * @param property property, changed in place
	 * @param value plain value
	 * @throws IllegalArgumentException if the type cannot be written or the value does not fit it
	 */
	public static void write(String tag, String part, Property property, Object value) {
		Property.Type type = property.getType();
		if (!WRITABLE.contains(type)) {
			throw new IllegalArgumentException(
					"Part '%s' of tag '%s' has the type %s, which cannot be written".formatted(part, tag, type));
		}
		Value v = new Value(tag, part, type, value);
		switch (type) {
		case STRING:
		case RICHTEXT:
		case FORM:
			property.setStringValue(v.string());
			break;
		case BOOLEAN:
			property.setBooleanValue(v.bool());
			break;
		case PAGE:
			if (value instanceof String url) {
				// an empty string, as read from an unset part, clears it
				property.setPageId(null);
				property.setStringValue(url);
			} else {
				property.setPageId(v.id(ObjectRef.Type.PAGE));
				property.setStringValue(null);
				Integer nodeId = v.nodeId();
				if (nodeId != null) {
					property.setNodeId(nodeId);
				}
			}
			break;
		case FILE:
			property.setFileId(v.id(ObjectRef.Type.FILE));
			break;
		case IMAGE:
			property.setImageId(v.id(ObjectRef.Type.IMAGE));
			break;
		case FOLDER:
			property.setFolderId(v.id(ObjectRef.Type.FOLDER));
			break;
		case NODE:
			property.setNodeId(v.id(ObjectRef.Type.NODE));
			break;
		case CMSFORM:
			property.setFormId(v.id(ObjectRef.Type.FORM));
			break;
		case LIST:
		case ORDEREDLIST:
		case UNORDEREDLIST:
			property.setStringValues(v.strings());
			break;
		case SELECT:
			property.setSelectedOptions(value == null ? new ArrayList<>()
					: new ArrayList<>(List.of(option(v, property, v.string()))));
			break;
		case MULTISELECT:
			List<SelectOption> selected = new ArrayList<>();
			for (String item : v.strings()) {
				selected.add(option(v, property, item));
			}
			property.setSelectedOptions(selected);
			break;
		case DATASOURCE:
			property.setOptions(entries(v, property.getOptions()));
			break;
		case OVERVIEW:
			property.setOverview(overview(v, property.getOverview()));
			break;
		case PAGETAG:
			Map<String, Object> pageTag = v.map(Set.of("pageId", "contentTagId", "templateTagId"));
			property.setPageId(v.member(pageTag, "pageId", true));
			property.setContentTagId(v.member(pageTag, "contentTagId", false));
			property.setTemplateTagId(v.member(pageTag, "templateTagId", false));
			if ((property.getContentTagId() == null) == (property.getTemplateTagId() == null)) {
				throw v.wrong("{pageId, contentTagId} or {pageId, templateTagId}");
			}
			break;
		case TEMPLATETAG:
			Map<String, Object> templateTag = v.map(Set.of("templateId", "templateTagId"));
			property.setTemplateId(v.member(templateTag, "templateId", true));
			property.setTemplateTagId(v.member(templateTag, "templateTagId", true));
			break;
		default:
			throw v.wrong("a writable value");
		}
	}

	/**
	 * Check whether a value read back after the save is the value that was written
	 * @param written property as written
	 * @param read property read back, may be null
	 * @return true if they are the same
	 */
	public static boolean same(Property written, Property read) {
		Object expected = TagInfo.value(written);
		Object actual = TagInfo.value(read);
		if (written.getType() == Property.Type.OVERVIEW && expected instanceof Map<?, ?> e
				&& actual instanceof Map<?, ?> a) {
			// the CMS fills what was not given (e.g. the order of a manual selection), only given members count
			for (Map.Entry<?, ?> member : e.entrySet()) {
				if (!Objects.equals(member.getValue(), a.get(member.getKey()))) {
					return false;
				}
			}
			return true;
		}
		return Objects.equals(expected, actual);
	}

	private static SelectOption option(Value v, Property property, String wanted) {
		List<SelectOption> options = property.getOptions() != null ? property.getOptions() : List.of();
		for (SelectOption option : options) {
			if (Objects.equals(option.getValue(), wanted)) {
				return option;
			}
		}
		for (SelectOption option : options) {
			if (Objects.equals(option.getKey(), wanted)) {
				return option;
			}
		}
		throw new IllegalArgumentException("Part '%s' of tag '%s' has no option '%s'; the options are: %s".formatted(
				v.part, v.tag, wanted, String.join(", ", options.stream().map(SelectOption::getValue).toList())));
	}

	private static List<SelectOption> entries(Value v, List<SelectOption> existing) {
		if (!(v.value instanceof List<?> items)) {
			throw v.wrong("an array of {key, value} entries");
		}
		List<SelectOption> entries = new ArrayList<>();
		for (Object item : items) {
			if (!(item instanceof Map<?, ?> entry) || !(entry.get("key") instanceof String key)
					|| !(entry.get("value") instanceof String value)
					|| !Set.of("key", "value").containsAll(entry.keySet())) {
				throw v.wrong("an array of {key, value} entries with string members");
			}
			SelectOption option = new SelectOption();
			// an entry with an existing key keeps its ID
			if (existing != null) {
				existing.stream().filter(o -> Objects.equals(o.getKey(), key)).findFirst()
						.ifPresent(o -> option.setId(o.getId()));
			}
			option.setKey(key);
			option.setValue(value);
			entries.add(option);
		}
		return entries;
	}

	private static Overview overview(Value v, Overview existing) {
		Map<String, Object> given = v.map(
				Set.of("listType", "selectType", "orderBy", "orderDirection", "maxItems", "recursive", "items"));
		Overview overview = existing != null ? existing : new Overview();
		if (given.containsKey("listType")) {
			overview.setListType(v.constant(Overview.ListType.class, given.get("listType"), "listType"));
		}
		if (given.containsKey("selectType")) {
			overview.setSelectType(v.constant(Overview.SelectType.class, given.get("selectType"), "selectType"));
		}
		if (given.containsKey("orderBy")) {
			overview.setOrderBy(v.constant(Overview.OrderBy.class, given.get("orderBy"), "orderBy"));
		}
		if (given.containsKey("orderDirection")) {
			overview.setOrderDirection(
					v.constant(Overview.OrderDirection.class, given.get("orderDirection"), "orderDirection"));
		}
		if (given.containsKey("maxItems")) {
			overview.setMaxItems(v.member(given, "maxItems", true));
		}
		if (given.containsKey("recursive")) {
			if (!(given.get("recursive") instanceof Boolean recursive)) {
				throw v.wrong("'recursive' as a boolean");
			}
			overview.setRecursive(recursive);
		}
		if (given.containsKey("items")) {
			if (!(given.get("items") instanceof List<?> items)) {
				throw v.wrong("'items' as an array of IDs or refs");
			}
			List<Integer> ids = new ArrayList<>();
			List<NodeIdObjectId> nodeIds = new ArrayList<>();
			for (Object item : items) {
				Value one = new Value(v.tag, v.part, v.type, item);
				int id = one.id(null);
				ids.add(id);
				nodeIds.add(new NodeIdObjectId(Objects.requireNonNullElse(one.nodeId(), 0), id));
			}
			// the CMS reads one of them, depending on whether the overview is sticky to a channel
			overview.setSelectedItemIds(ids);
			overview.setSelectedNodeItemIds(nodeIds);
		}
		return overview;
	}

	/**
	 * Read the overview of a property as a plain value, see {@link #VALUE_SHAPES}
	 * @param overview REST overview, may be null
	 * @return value or null
	 */
	public static Map<String, Object> overviewValue(Overview overview) {
		if (overview == null) {
			return null;
		}
		Map<String, Object> value = new LinkedHashMap<>();
		putIfDefined(value, "listType", overview.getListType());
		putIfDefined(value, "selectType", overview.getSelectType());
		putIfDefined(value, "orderBy", overview.getOrderBy());
		putIfDefined(value, "orderDirection", overview.getOrderDirection());
		if (overview.getMaxItems() != null) {
			value.put("maxItems", overview.getMaxItems());
		}
		if (overview.isRecursive() != null) {
			value.put("recursive", overview.isRecursive());
		}
		ObjectRef.Type itemType = itemType(overview.getListType());
		List<ObjectRef> items = new ArrayList<>();
		if (overview.getSelectedNodeItemIds() != null && !overview.getSelectedNodeItemIds().isEmpty()) {
			for (NodeIdObjectId item : overview.getSelectedNodeItemIds()) {
				items.add(new ObjectRef(itemType, item.getObjectId(), null,
						item.getNodeId() > 0 ? item.getNodeId() : null, null, null, null, null, null));
			}
		} else if (overview.getSelectedItemIds() != null) {
			for (Integer id : overview.getSelectedItemIds()) {
				items.add(ObjectRef.of(itemType, id));
			}
		}
		value.put("items", items);
		return value;
	}

	private static ObjectRef.Type itemType(Overview.ListType listType) {
		if (listType == null) {
			return null;
		}
		return switch (listType) {
		case PAGE -> ObjectRef.Type.PAGE;
		case FOLDER -> ObjectRef.Type.FOLDER;
		case IMAGE -> ObjectRef.Type.IMAGE;
		case FILE -> ObjectRef.Type.FILE;
		default -> null;
		};
	}

	private static void putIfDefined(Map<String, Object> value, String key, Enum<?> constant) {
		if (constant != null && !"UNDEFINED".equals(constant.name())) {
			value.put(key, constant.name());
		}
	}

	/**
	 * A value to write, with its tag and part for messages
	 */
	private record Value(String tag, String part, Property.Type type, Object value) {
		IllegalArgumentException wrong(String expected) {
			return new IllegalArgumentException("Part '%s' of tag '%s' has the type %s and needs %s, not %s"
					.formatted(part, tag, type, expected, value));
		}

		String string() {
			if (!(value instanceof String string)) {
				throw wrong("a string");
			}
			return string;
		}

		boolean bool() {
			if (!(value instanceof Boolean bool)) {
				throw wrong("a boolean");
			}
			return bool;
		}

		List<String> strings() {
			if (!(value instanceof List<?> list) || !list.stream().allMatch(String.class::isInstance)) {
				throw wrong("an array of strings");
			}
			return list.stream().map(String.class::cast).toList();
		}

		/**
		 * The object ID of a plain ID or a ref {type, id}
		 * @param expected expected ref type, null for any
		 */
		int id(ObjectRef.Type expected) {
			Object id = value;
			if (value instanceof Map<?, ?> ref) {
				Object type = ref.get("type");
				if (expected != null && type != null && !expected.value().equals(type)) {
					throw wrong("a positive integer ID or a ref of type %s".formatted(expected.value()));
				}
				id = ref.get("id");
			}
			if (id instanceof Number number && number.doubleValue() == number.intValue() && number.intValue() > 0) {
				return number.intValue();
			}
			throw wrong("a positive integer ID or a ref {type, id}");
		}

		/**
		 * The node ID of a ref, null for a plain ID or a ref without it
		 */
		Integer nodeId() {
			if (value instanceof Map<?, ?> ref && ref.get("nodeId") instanceof Number number) {
				return number.intValue();
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> map(Set<String> members) {
			if (!(value instanceof Map<?, ?> map) || !members.containsAll(map.keySet())) {
				throw wrong("an object with the members " + String.join(", ", members.stream().sorted().toList()));
			}
			return (Map<String, Object>) map;
		}

		Integer member(Map<String, Object> map, String name, boolean required) {
			Object member = map.get(name);
			if (member == null && !required) {
				return null;
			}
			if (member instanceof Number number && number.doubleValue() == number.intValue()
					&& number.intValue() >= 0) {
				return number.intValue();
			}
			throw wrong("'%s' as a non-negative integer".formatted(name));
		}

		<E extends Enum<E>> E constant(Class<E> type, Object member, String name) {
			try {
				return Enum.valueOf(type, String.valueOf(member));
			} catch (IllegalArgumentException e) {
				throw wrong("'%s' as one of %s".formatted(name, String.join(", ",
						EnumSet.allOf(type).stream().map(Enum::name).filter(n -> !"UNDEFINED".equals(n)).toList())));
			}
		}
	}
}
