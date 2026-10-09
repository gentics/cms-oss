package com.gentics.contentnode.mcp.tools;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.PageInfo;
import com.gentics.contentnode.mcp.model.TagInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.Constructs;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Writes tag values of a page in one lock cycle: {@link PageResourceImpl#load} with {@code update} (edit permission,
 * lock), then {@link PageResourceImpl#save} with {@code unlock}. Every value is checked against the type of its part
 * before the save, because the CMS silently skips a value of the wrong type or for an unknown part, and read back after
 * it. Missing tags are created in the same save, under the given name.
 */
public class UpdatePageTagsTool extends AbstractMcpTool {
	static final String NAME = "update_page_tags";

	static final String ARG_PAGE_ID = "pageId";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_TAGS = "tags";

	static final String ARG_CREATE_MISSING = "createMissing";

	static final String ARG_DELETE_TAGS = "deleteTags";

	static final String ARG_CREATE_VERSION = "createVersion";

	/**
	 * Maximum number of tags and of parts per tag
	 */
	static final int MAX_ITEMS = 100;

	/**
	 * Maximum number of tags to delete
	 */
	static final int MAX_DELETE_TAGS = 50;

	/**
	 * Maximum length of tag names and construct keywords
	 */
	static final int MAX_NAME_LENGTH = 100;

	/**
	 * Maximum size of all values, in UTF-8 bytes
	 */
	static final int MAX_VALUE_BYTES = 200 * 1024;

	/**
	 * Part types whose values can be written
	 */
	static final Set<Property.Type> SUPPORTED_TYPES = EnumSet.of(Property.Type.STRING, Property.Type.RICHTEXT,
			Property.Type.BOOLEAN, Property.Type.PAGE, Property.Type.FILE, Property.Type.IMAGE, Property.Type.FOLDER);

	/**
	 * Values to write into one tag
	 * @param constructKeyword construct of the tag, if it is created, may be null
	 * @param properties values by part keyword
	 */
	record TagValues(String constructKeyword, Map<String, Object> properties) {
	}

	/**
	 * Validated arguments
	 * @param pageId page ID
	 * @param tags values by tag name
	 * @param createMissing whether to create missing tags
	 * @param deleteTags names of tags to delete
	 * @param createVersion whether to create a page version
	 */
	record Request(int pageId, Map<String, TagValues> tags, boolean createMissing, List<String> deleteTags,
			boolean createVersion) {
		/**
		 * Validate the arguments
		 * @param arguments arguments
		 * @return request
		 * @throws IllegalArgumentException for missing or invalid arguments
		 */
		@SuppressWarnings("unchecked")
		static Request of(Map<String, Object> arguments) {
			int pageId = Args.id(arguments, ARG_PAGE_ID);
			if (!(arguments.get(ARG_TAGS) instanceof Map<?, ?> tagsArg) || tagsArg.isEmpty()
					|| tagsArg.size() > MAX_ITEMS) {
				throw new IllegalArgumentException(
						"Argument '%s' must be an object with 1 to %d tags".formatted(ARG_TAGS, MAX_ITEMS));
			}
			List<String> deleteTags = Args.stringList(arguments, ARG_DELETE_TAGS, MAX_DELETE_TAGS, MAX_NAME_LENGTH);
			if (deleteTags == null) {
				deleteTags = List.of();
			}

			Map<String, TagValues> tags = new LinkedHashMap<>();
			int bytes = 0;
			for (Map.Entry<?, ?> entry : tagsArg.entrySet()) {
				String name = String.valueOf(entry.getKey());
				if (name.isBlank() || name.length() > MAX_NAME_LENGTH) {
					throw new IllegalArgumentException("Tag name '%s' must have 1 to %d characters".formatted(name,
							MAX_NAME_LENGTH));
				}
				if (deleteTags.contains(name)) {
					throw new IllegalArgumentException("Tag '%s' is both written and deleted".formatted(name));
				}
				if (!(entry.getValue() instanceof Map<?, ?> tagArg)) {
					throw new IllegalArgumentException("Tag '%s' must be an object".formatted(name));
				}
				Map<String, Object> tag = (Map<String, Object>) tagArg;
				for (String key : tag.keySet()) {
					if (!"constructKeyword".equals(key) && !"properties".equals(key)) {
						throw new IllegalArgumentException("Tag '%s' has the unknown member '%s'".formatted(name, key));
					}
				}
				if (!(tag.get("properties") instanceof Map<?, ?> properties) || properties.isEmpty()
						|| properties.size() > MAX_ITEMS) {
					throw new IllegalArgumentException(
							"Tag '%s' needs 'properties' with 1 to %d parts".formatted(name, MAX_ITEMS));
				}
				for (Object value : properties.values()) {
					bytes += String.valueOf(value).getBytes(UTF_8).length;
				}
				tags.put(name, new TagValues(nullIfBlank(stringArg(tag, "constructKeyword", 0, MAX_NAME_LENGTH)),
						(Map<String, Object>) properties));
			}
			if (bytes > MAX_VALUE_BYTES) {
				throw new IllegalArgumentException(
						"The values have %d bytes, at most %d are allowed".formatted(bytes, MAX_VALUE_BYTES));
			}

			return new Request(pageId, tags, booleanArg(arguments, ARG_CREATE_MISSING, false), deleteTags,
					booleanArg(arguments, ARG_CREATE_VERSION, true));
		}
	}

	/**
	 * Construct of a tag to create
	 * @param constructId construct ID
	 * @param parts part types by part keyword
	 */
	record NewTag(int constructId, Map<String, Property.Type> parts) {
	}

	/**
	 * Looks up the construct of a tag to create
	 */
	interface ConstructLookup {
		/**
		 * Get the construct by keyword
		 * @param keyword construct keyword
		 * @return construct ID and parts
		 * @throws Exception if there is no such construct
		 */
		NewTag of(String keyword) throws Exception;
	}

	/**
	 * Result of the tool
	 * @param page page after the update
	 * @param changedTags written tags whose values changed
	 * @param createdTags created tags
	 * @param deletedTags deleted tags
	 * @param unchangedTags written tags whose values did not change
	 */
	public record Result(PageInfo page, List<String> changedTags, List<String> createdTags, List<String> deletedTags,
			List<String> unchangedTags) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> tagProperties = new LinkedHashMap<>();
		tagProperties.put("constructKeyword", schema("string",
				"Required only when createMissing is true and the tag does not exist yet.", "maxLength",
				MAX_NAME_LENGTH));
		tagProperties.put("properties", schema("object",
				"partKeyword -> value. Strings for text and rich-text parts, booleans for checkboxes, integer ids for "
						+ "page, file, image and folder parts. Other part types cannot be written.",
				"minProperties", 1, "maxProperties", MAX_ITEMS, "additionalProperties", true));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_PAGE_ID, schema("integer", "ID of the page.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Not supported yet: calls that set it are rejected.", "minimum",
				1));
		properties.put(ARG_TAGS, schema("object", "Keyed by tag name (CMS Tag.name), e.g. content or genaix_hero_1.",
				"minProperties", 1, "maxProperties", MAX_ITEMS, "additionalProperties",
				schema("object", null, "properties", tagProperties, "required", List.of("properties"),
						"additionalProperties", false)));
		properties.put(ARG_CREATE_MISSING, schema("boolean",
				"Create a tag that does not exist yet, under the given name, from its constructKeyword.", "default",
				false));
		properties.put(ARG_DELETE_TAGS, schema("array", "Names of tags to delete.", "maxItems", MAX_DELETE_TAGS,
				"uniqueItems", true, "items", schema("string", null, "maxLength", MAX_NAME_LENGTH)));
		properties.put(ARG_CREATE_VERSION, schema("boolean", "Create a page version.", "default", true));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		return Tool.builder().name(NAME).title("Update page tags")
				.description("Sets the values of one or more content blocks on a page in a single call. This is the "
						+ "only way to write page content. The tool takes the edit lock, applies exactly the values you "
						+ "give, saves, creates a version and releases the lock, so you never have to lock or unlock "
						+ "anything yourself. Values are written byte for byte and read back. Name only the tags and "
						+ "parts you want to change; untouched tags are left alone. An unknown part or a value of the "
						+ "wrong type is refused and nothing is written. Use add_page_tag first if a tag does not exist "
						+ "yet, or set createMissing with a constructKeyword. Do NOT use this tool for page metadata "
						+ "such as name or filename, that is update_page_properties.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_PAGE_ID, ARG_TAGS)).additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Writes.rejectChannel(NAME, arguments);
		Request request = Request.of(arguments);
		Writes.logIdempotencyKey(NAME, request.pageId(), arguments);
		String id = Integer.toString(request.pageId());

		PageResource pageResource = RestPermissions.guard(PageResource.class, new PageResourceImpl());
		// load for update, which checks the edit permission and locks the page
		PageLoadResponse loaded = pageResource.load(id, true, false, false, false, false, false, false, false, false,
				false, null, null);
		requireOk(loaded, "The page %s could not be loaded".formatted(id));
		Map<String, Tag> before = tags(loaded.getPage());
		// flattened before the values are set into the loaded properties
		Map<String, Map<String, Object>> beforeValues = flatten(before);
		List<String> deleted = request.deleteTags().stream().distinct().filter(before::containsKey).toList();

		saveOrReleaseLock(com.gentics.contentnode.object.Page.class, id, "Page %s was not saved".formatted(id), () -> {
			Page page = new Page();
			page.setId(request.pageId());
			page.setTags(restTags(before, request, UpdatePageTagsTool::construct));
			PageSaveRequest save = new PageSaveRequest(page);
			save.setUnlock(true);
			save.setCreateVersion(request.createVersion());
			save.setDelete(deleted.isEmpty() ? null : deleted);
			return pageResource.save(id, save);
		});

		Page after = Writes.readBack(pageResource, request.pageId());
		List<String> differences = differences(request.tags(), tags(after));
		if (!differences.isEmpty()) {
			throw new IllegalStateException("Page %s was saved, but these values read back differently: %s"
					.formatted(id, String.join("; ", differences)));
		}
		return result(beforeValues, after, request.tags().keySet(), deleted);
	}

	/**
	 * Build the REST tags to save, with every value checked against the type of its part
	 * @param loaded tags of the page by name
	 * @param request request
	 * @param constructs looks up the construct of a tag to create
	 * @return REST tags by name
	 * @throws Exception
	 * @throws IllegalArgumentException for an unknown tag or part, or a value that does not fit its part
	 */
	static Map<String, Tag> restTags(Map<String, Tag> loaded, Request request, ConstructLookup constructs)
			throws Exception {
		Map<String, Tag> tags = new LinkedHashMap<>();
		for (Map.Entry<String, TagValues> entry : request.tags().entrySet()) {
			String name = entry.getKey();
			Tag existing = loaded.get(name);
			Tag tag = new Tag();
			tag.setName(name);
			Map<String, Property> properties = new LinkedHashMap<>();

			if (existing != null) {
				// copied, so the CMS does not re-activate an inactive tag
				tag.setActive(existing.getActive());
				Map<String, Property> parts = existing.getProperties() != null ? existing.getProperties() : Map.of();
				for (Map.Entry<String, Object> value : entry.getValue().properties().entrySet()) {
					Property property = parts.get(value.getKey());
					if (property == null) {
						throw unknownPart(name, value.getKey(), parts.keySet());
					}
					set(name, value.getKey(), property, value.getValue());
					properties.put(value.getKey(), property);
				}
			} else {
				String keyword = entry.getValue().constructKeyword();
				if (!request.createMissing() || keyword == null || name.startsWith("object.")) {
					throw new IllegalArgumentException(("The page has no tag '%s'. Add it with add_page_tag, or pass "
							+ "createMissing with a constructKeyword to create a content tag of that name.")
							.formatted(name));
				}
				NewTag construct = constructs.of(keyword);
				tag.setConstructId(construct.constructId());
				tag.setActive(true);
				for (Map.Entry<String, Object> value : entry.getValue().properties().entrySet()) {
					Property.Type type = construct.parts().get(value.getKey());
					if (type == null) {
						throw unknownPart(name, value.getKey(), construct.parts().keySet());
					}
					Property property = new Property();
					property.setType(type);
					set(name, value.getKey(), property, value.getValue());
					properties.put(value.getKey(), property);
				}
			}
			tag.setProperties(properties);
			tags.put(name, tag);
		}
		return tags;
	}

	/**
	 * Set a value into a property, by the property's type
	 * @param tag tag name
	 * @param part part keyword
	 * @param property property
	 * @param value value
	 * @throws IllegalArgumentException if the type is not supported or the value does not fit it
	 */
	static void set(String tag, String part, Property property, Object value) {
		Property.Type type = property.getType();
		if (!SUPPORTED_TYPES.contains(type)) {
			throw new IllegalArgumentException(
					"Part '%s' of tag '%s' has the type %s, which cannot be written".formatted(part, tag, type));
		}
		switch (type) {
		case STRING:
		case RICHTEXT:
			if (!(value instanceof String string)) {
				throw wrongType(tag, part, type, "a string", value);
			}
			property.setStringValue(string);
			break;
		case BOOLEAN:
			if (!(value instanceof Boolean bool)) {
				throw wrongType(tag, part, type, "a boolean", value);
			}
			property.setBooleanValue(bool);
			break;
		default:
			if (!(value instanceof Integer objectId) || objectId < 1) {
				throw wrongType(tag, part, type, "a positive integer ID", value);
			}
			switch (type) {
			case PAGE:
				property.setPageId(objectId);
				break;
			case FILE:
				property.setFileId(objectId);
				break;
			case IMAGE:
				property.setImageId(objectId);
				break;
			default:
				property.setFolderId(objectId);
				break;
			}
		}
	}

	/**
	 * Compare the written values with the values read back
	 * @param written written values by tag name
	 * @param after tags after the save by name
	 * @return differences, empty if every value was read back unchanged
	 */
	static List<String> differences(Map<String, TagValues> written, Map<String, Tag> after) {
		List<String> differences = new ArrayList<>();
		for (Map.Entry<String, TagValues> tag : written.entrySet()) {
			Tag saved = after.get(tag.getKey());
			for (Map.Entry<String, Object> part : tag.getValue().properties().entrySet()) {
				Property property = saved != null && saved.getProperties() != null
						? saved.getProperties().get(part.getKey())
						: null;
				Object actual = TagInfo.value(property);
				Object expected = part.getValue();
				boolean same = actual instanceof ObjectRef ref
						? expected instanceof Integer objectId && ref.id() == objectId
						: Objects.equals(expected, actual);
				if (!same) {
					differences.add("%s.%s is %s".formatted(tag.getKey(), part.getKey(), actual));
				}
			}
		}
		return differences;
	}

	/**
	 * Build the result
	 * @param before flattened values of the tags before the save
	 * @param after page after the save
	 * @param written names of the written tags
	 * @param deleted names of the deleted tags
	 * @return result
	 */
	static Result result(Map<String, Map<String, Object>> before, Page after, Set<String> written,
			List<String> deleted) {
		Map<String, Map<String, Object>> afterValues = flatten(tags(after));
		List<String> changed = new ArrayList<>();
		List<String> created = new ArrayList<>();
		List<String> unchanged = new ArrayList<>();
		for (String name : written) {
			if (!before.containsKey(name)) {
				created.add(name);
			} else if (Objects.equals(before.get(name), afterValues.get(name))) {
				unchanged.add(name);
			} else {
				changed.add(name);
			}
		}
		return new Result(PageInfo.of(after), changed, created, deleted, unchanged);
	}

	/**
	 * Get the tags of a page
	 * @param page REST page
	 * @return tags by name, empty if the page has none
	 */
	private static Map<String, Tag> tags(Page page) {
		return page.getTags() != null ? page.getTags() : Map.of();
	}

	/**
	 * Flatten the values of tags, see {@link TagInfo#value}
	 * @param tags tags by name
	 * @return values by part keyword, by tag name
	 */
	private static Map<String, Map<String, Object>> flatten(Map<String, Tag> tags) {
		Map<String, Map<String, Object>> values = new LinkedHashMap<>();
		for (Map.Entry<String, Tag> tag : tags.entrySet()) {
			values.put(tag.getKey(), TagInfo.of(tag.getValue(), Map.of(), false).properties());
		}
		return values;
	}

	/**
	 * Look up a construct by keyword, in its own transaction
	 * @param keyword construct keyword
	 * @return construct ID and part types
	 * @throws Exception if there is no such construct
	 */
	private static NewTag construct(String keyword) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Construct construct = Constructs.byKeyword(keyword);
			Map<String, Property.Type> parts = new LinkedHashMap<>();
			for (Part part : construct.getParts()) {
				if (part.getKeyname() != null && !part.getKeyname().isBlank()) {
					parts.put(part.getKeyname(), Property.Type.get(part.getPartTypeId()));
				}
			}
			trx.success();
			return new NewTag(construct.getId(), parts);
		}
	}

	private static IllegalArgumentException unknownPart(String tag, String part, Set<String> parts) {
		return new IllegalArgumentException("Tag '%s' has no part '%s', its parts are %s".formatted(tag, part, parts));
	}

	private static IllegalArgumentException wrongType(String tag, String part, Property.Type type, String expected,
			Object value) {
		return new IllegalArgumentException("Part '%s' of tag '%s' has the type %s and needs %s, not %s"
				.formatted(part, tag, type, expected, value));
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> names = schema("array", null, "items", schema("string", null));
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("page", PageInfo.jsonSchema("The page after the update."));
		properties.put("changedTags", names);
		properties.put("createdTags", names);
		properties.put("deletedTags", names);
		properties.put("unchangedTags", names);
		return schema("object", null, "properties", properties, "required", List.of("page", "changedTags"));
	}
}
