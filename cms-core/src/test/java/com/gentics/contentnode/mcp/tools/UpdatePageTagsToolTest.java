package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.tools.UpdatePageTagsTool.ConstructLookup;
import com.gentics.contentnode.mcp.tools.UpdatePageTagsTool.NewTag;
import com.gentics.contentnode.mcp.tools.UpdatePageTagsTool.Request;
import com.gentics.contentnode.mcp.tools.UpdatePageTagsTool.TagValues;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;

/**
 * Unit tests for {@link UpdatePageTagsTool}. Writing a real page is covered by the live check.
 */
public class UpdatePageTagsToolTest {
	private static final ConstructLookup NO_CONSTRUCTS = keyword -> {
		throw new AssertionError("no construct lookup expected");
	};

	private final UpdatePageTagsTool tool = new UpdatePageTagsTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "update_page_tags", "Update page tags", "pageId", "tags");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("pageId", 4711, "tags", Map.of("content1", Map.of("properties", Map.of("text",
				"x")))));
		assertAccepted(tool, Map.of("pageId", 4711, "tags", Map.of("hero", Map.of("constructKeyword", "hero",
				"properties", Map.of("text", "x"))), "createMissing", true, "deleteTags", List.of("old"),
				"createVersion", false));
		assertRejected(tool, Map.of("pageId", 4711));
		assertRejected(tool, Map.of("pageId", 4711, "tags", Map.of()));
		assertRejected(tool, Map.of("pageId", 4711, "tags", Map.of("content1", Map.of())));
		assertRejected(tool, Map.of("pageId", 4711, "tags", Map.of("content1", Map.of("properties", Map.of(),
				"x", 1))));
		assertRejected(tool, Map.of("pageId", 4711, "tags", Map.of("c", Map.of("properties", Map.of("t", "x"))),
				"deleteTags", List.of("a", "a")));
	}

	@Test
	public void testRejectsChannel() {
		assertThatThrownBy(() -> tool.invoke(Map.of("pageId", 4711, "nodeId", 2, "tags", Map.of()), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("support nodeId");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool,
				Map.of("pageId", 4711, "tags", Map.of("content1", Map.of("properties", Map.of("text", "x")))));
	}

	@Test
	public void testRequestRejections() {
		assertRequestRejected(Map.of("pageId", 4711), "'tags'");
		assertRequestRejected(Map.of("pageId", 4711, "tags", Map.of()), "'tags'");
		assertRequestRejected(Map.of("pageId", 4711, "tags", Map.of("c", Map.of())), "needs 'properties'");
		assertRequestRejected(Map.of("pageId", 4711, "tags", Map.of("c", Map.of("properties", Map.of("t", "x"),
				"name", "y"))), "unknown member 'name'");
		assertRequestRejected(Map.of("pageId", 4711, "tags", Map.of("c", Map.of("properties", Map.of("t", "x"))),
				"deleteTags", List.of("c")), "both written and deleted");
		assertRequestRejected(Map.of("pageId", 4711, "tags", Map.of("c", Map.of("properties", Map.of("t",
				"x".repeat(200 * 1024 + 1))))), "bytes");
	}

	@Test
	public void testRequest() {
		Request request = Request.of(Map.of("pageId", 4711, "tags", Map.of("c", Map.of("properties",
				Map.of("t", "x")))));

		assertThat(request.pageId()).isEqualTo(4711);
		assertThat(request.tags()).containsOnlyKeys("c");
		assertThat(request.createMissing()).isFalse();
		assertThat(request.createVersion()).isTrue();
		assertThat(request.deleteTags()).isEmpty();
	}

	@Test
	public void testRestTags() throws Exception {
		Tag content = tag("content1", false, property(Property.Type.RICHTEXT, "old"),
				property(Property.Type.BOOLEAN, null));
		Map<String, Tag> rest = UpdatePageTagsTool.restTags(Map.of("content1", content),
				request(false, "content1", null, "text", "<p>a&nbsp;b\r\n</p>", "flag", true), NO_CONSTRUCTS);

		Tag tag = rest.get("content1");
		assertThat(tag.getName()).isEqualTo("content1");
		assertThat(tag.getActive()).isFalse();
		assertThat(tag.getProperties().get("text").getStringValue()).isEqualTo("<p>a&nbsp;b\r\n</p>");
		assertThat(tag.getProperties().get("text").getType()).isEqualTo(Property.Type.RICHTEXT);
		assertThat(tag.getProperties().get("flag").getBooleanValue()).isTrue();
	}

	@Test
	public void testRestTagsReferences() throws Exception {
		Tag link = tag("link", true, property(Property.Type.PAGE, null), property(Property.Type.IMAGE, null));
		Map<String, Tag> rest = UpdatePageTagsTool.restTags(Map.of("link", link),
				request(false, "link", null, "text", 17, "flag", 23), NO_CONSTRUCTS);

		assertThat(rest.get("link").getProperties().get("text").getPageId()).isEqualTo(17);
		assertThat(rest.get("link").getProperties().get("flag").getImageId()).isEqualTo(23);
	}

	@Test
	public void testRestTagsRejections() {
		Map<String, Tag> loaded = Map.of("content1", tag("content1", true, property(Property.Type.STRING, "old"),
				property(Property.Type.MULTISELECT, null)));

		assertRestTagsRejected(loaded, request(false, "content1", null, "txet", "x"), "has no part 'txet'");
		assertRestTagsRejected(loaded, request(false, "content1", null, "text", 5), "needs a string");
		assertRestTagsRejected(loaded, request(false, "content1", null, "flag", List.of("a")),
				"MULTISELECT, which cannot be written");
		assertRestTagsRejected(loaded, request(false, "hero1", "hero", "text", "x"), "has no tag 'hero1'");
		assertRestTagsRejected(loaded, request(true, "hero1", null, "text", "x"), "has no tag 'hero1'");
		assertRestTagsRejected(loaded, request(true, "object.x", "hero", "text", "x"), "has no tag 'object.x'");
	}

	@Test
	public void testCreateMissing() throws Exception {
		ConstructLookup constructs = keyword -> {
			assertThat(keyword).isEqualTo("hero");
			return new NewTag(12, Map.of("text", Property.Type.STRING));
		};
		Map<String, Tag> rest = UpdatePageTagsTool.restTags(Map.of(), request(true, "genaix_hero_1", "hero", "text",
				"Hello"), constructs);

		Tag tag = rest.get("genaix_hero_1");
		assertThat(tag.getName()).isEqualTo("genaix_hero_1");
		assertThat(tag.getConstructId()).isEqualTo(12);
		assertThat(tag.getActive()).isTrue();
		assertThat(tag.getProperties().get("text").getType()).isEqualTo(Property.Type.STRING);
		assertThat(tag.getProperties().get("text").getStringValue()).isEqualTo("Hello");

		assertThatThrownBy(() -> UpdatePageTagsTool.restTags(Map.of(),
				request(true, "genaix_hero_1", "hero", "txet", "Hello"), constructs))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("has no part 'txet'");
	}

	@Test
	public void testDifferences() {
		Map<String, Tag> after = Map.of("content1", tag("content1", true, property(Property.Type.STRING, "a b"),
				pageProperty(17)));

		assertThat(UpdatePageTagsTool.differences(Map.of("content1", values("text", "a b", "flag", 17)), after))
				.isEmpty();
		assertThat(UpdatePageTagsTool.differences(Map.of("content1", values("text", "a b", "flag", 18)), after))
				.containsExactly("content1.text is a b",
						"content1.flag is %s".formatted(ObjectRef.of(ObjectRef.Type.PAGE, 17)));
		assertThat(UpdatePageTagsTool.differences(Map.of("missing", values("text", "x")), after))
				.containsExactly("missing.text is null");
	}

	@Test
	public void testResult() {
		Map<String, Map<String, Object>> before = Map.of("changed", Map.of("text", "old"), "same",
				Map.of("text", "same"));
		Page after = new Page();
		after.setId(4711);
		Map<String, Tag> tags = new HashMap<>();
		tags.put("changed", tag("changed", true, property(Property.Type.STRING, "new")));
		tags.put("same", tag("same", true, property(Property.Type.STRING, "same")));
		tags.put("created", tag("created", true, property(Property.Type.STRING, "x")));
		after.setTags(tags);

		UpdatePageTagsTool.Result result = UpdatePageTagsTool.result(before, after,
				new LinkedHashSet<>(List.of("changed", "same", "created")), List.of("old"));

		assertThat(result.changedTags()).containsExactly("changed");
		assertThat(result.unchangedTags()).containsExactly("same");
		assertThat(result.createdTags()).containsExactly("created");
		assertThat(result.deletedTags()).containsExactly("old");
		assertValidOutput(tool, result);
	}

	private static void assertRequestRejected(Map<String, Object> arguments, String message) {
		assertThatThrownBy(() -> Request.of(arguments)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(message);
	}

	private static void assertRestTagsRejected(Map<String, Tag> loaded, Request request, String message) {
		assertThatThrownBy(() -> UpdatePageTagsTool.restTags(loaded, request, NO_CONSTRUCTS))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
	}

	private static Request request(boolean createMissing, String tag, String keyword, Object... partValues) {
		return new Request(4711, Map.of(tag, new TagValues(keyword, values(partValues).properties())), createMissing,
				List.of(), true);
	}

	private static TagValues values(Object... partValues) {
		Map<String, Object> properties = new LinkedHashMap<>();
		for (int i = 0; i < partValues.length; i += 2) {
			properties.put((String) partValues[i], partValues[i + 1]);
		}
		return new TagValues(null, properties);
	}

	/**
	 * Build a tag with the parts "text" and (if given) "flag"
	 * @param name tag name
	 * @param active whether active
	 * @param properties properties of "text" and "flag"
	 * @return tag
	 */
	private static Tag tag(String name, boolean active, Property... properties) {
		Tag tag = new Tag();
		tag.setName(name);
		tag.setConstructId(2);
		tag.setActive(active);
		tag.setType(Tag.Type.CONTENTTAG);
		Map<String, Property> map = new LinkedHashMap<>();
		map.put("text", properties[0]);
		if (properties.length > 1) {
			map.put("flag", properties[1]);
		}
		tag.setProperties(map);
		return tag;
	}

	private static Property property(Property.Type type, String value) {
		Property property = new Property();
		property.setType(type);
		property.setStringValue(value);
		return property;
	}

	private static Property pageProperty(int pageId) {
		Property property = property(Property.Type.PAGE, null);
		property.setPageId(pageId);
		return property;
	}
}
