package com.gentics.contentnode.mcp.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.SelectOption;
import com.gentics.contentnode.rest.model.Tag;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;

/**
 * Unit tests for {@link TagInfo}. Loading the construct keywords needs a CMS transaction and is covered by the live
 * check.
 */
public class TagInfoTest {
	@Test
	public void testText() {
		assertThat(TagInfo.value(property(Property.Type.STRING, p -> p.setStringValue("Hello")))).isEqualTo("Hello");
		assertThat(TagInfo.value(property(Property.Type.RICHTEXT, p -> p.setStringValue("<b>Hi</b>"))))
				.isEqualTo("<b>Hi</b>");
	}

	@Test
	public void testBoolean() {
		assertThat(TagInfo.value(property(Property.Type.BOOLEAN, p -> p.setBooleanValue(true)))).isEqualTo(true);
	}

	@Test
	public void testPageUrl() {
		assertThat(TagInfo.value(property(Property.Type.PAGE, p -> p.setPageId(4711))))
				.isEqualTo(ObjectRef.of(Type.PAGE, 4711));
		assertThat(TagInfo.value(property(Property.Type.PAGE, p -> {
			p.setPageId(0);
			p.setStringValue("https://www.gentics.com");
		}))).isEqualTo("https://www.gentics.com");
	}

	@Test
	public void testObjectUrls() {
		assertThat(TagInfo.value(property(Property.Type.FILE, p -> p.setFileId(12))))
				.isEqualTo(ObjectRef.of(Type.FILE, 12));
		assertThat(TagInfo.value(property(Property.Type.IMAGE, p -> p.setImageId(77))))
				.isEqualTo(ObjectRef.of(Type.IMAGE, 77));
		assertThat(TagInfo.value(property(Property.Type.FOLDER, p -> p.setFolderId(57))))
				.isEqualTo(ObjectRef.of(Type.FOLDER, 57));
		assertThat(TagInfo.value(property(Property.Type.CMSFORM, p -> p.setFormId(5))))
				.isEqualTo(ObjectRef.of(Type.FORM, 5));
		assertThat(TagInfo.value(property(Property.Type.NODE, p -> p.setNodeId(3))))
				.isEqualTo(ObjectRef.of(Type.NODE, 3));
		assertThat(TagInfo.value(property(Property.Type.IMAGE, p -> p.setImageId(0)))).isNull();
	}

	@Test
	public void testList() {
		assertThat(TagInfo.value(property(Property.Type.UNORDEREDLIST, p -> p.setStringValues(List.of("a", "b")))))
				.isEqualTo(List.of("a", "b"));
	}

	@Test
	public void testSelects() {
		List<SelectOption> selected = List.of(new SelectOption().setId(1).setKey("l").setValue("left"),
				new SelectOption().setId(2).setKey("r").setValue("right"));

		assertThat(TagInfo.value(property(Property.Type.SELECT, p -> p.setSelectedOptions(selected.subList(0, 1)))))
				.isEqualTo("left");
		assertThat(TagInfo.value(property(Property.Type.SELECT, p -> p.setSelectedOptions(List.of())))).isNull();
		assertThat(TagInfo.value(property(Property.Type.MULTISELECT, p -> p.setSelectedOptions(selected))))
				.isEqualTo(List.of("left", "right"));
	}

	@Test
	public void testTagLink() {
		assertThat(TagInfo.value(property(Property.Type.PAGETAG, p -> {
			p.setPageId(4711);
			p.setContentTagId(99);
		}))).isEqualTo(Map.of("pageId", 4711, "contentTagId", 99));
	}

	@Test
	public void testNoPlainValue() {
		assertThat(TagInfo.value(property(Property.Type.OVERVIEW, p -> {
		}))).isNull();
		assertThat(TagInfo.value(null)).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOf() {
		Tag tag = tag();

		TagInfo info = TagInfo.of(tag, Map.of(8, "text"), false);

		assertThat(info.name()).isEqualTo("content");
		assertThat(info.constructKeyword()).isEqualTo("text");
		assertThat(info.properties()).containsExactly(Map.entry("text", "Hello"));
		assertThat(info.propertiesRaw()).isNull();
		Map<String, Object> json = new ObjectMapper().convertValue(info, Map.class);
		assertThat(json).containsEntry("type", "CONTENTTAG").doesNotContainKey("propertiesRaw");
		ValidationResponse validation = new DefaultJsonSchemaValidator().validate(TagInfo.jsonSchema(), json);
		assertThat(validation.valid()).as(validation.errorMessage()).isTrue();
	}

	@Test
	public void testOfPrefersLoadedConstruct() {
		Tag tag = tag();
		Construct construct = new Construct();
		construct.setKeyword("loaded");
		tag.setConstruct(construct);

		TagInfo info = TagInfo.of(tag, Map.of(), true);

		assertThat(info.constructKeyword()).isEqualTo("loaded");
		assertThat(info.propertiesRaw()).isSameAs(tag.getProperties());
	}

	private static Tag tag() {
		Map<String, Property> properties = new LinkedHashMap<>();
		properties.put("text", property(Property.Type.STRING, p -> p.setStringValue("Hello")));
		properties.put("overview", property(Property.Type.OVERVIEW, p -> {
		}));
		Tag tag = new Tag();
		tag.setName("content");
		tag.setConstructId(8);
		tag.setType(Tag.Type.CONTENTTAG);
		tag.setActive(true);
		tag.setProperties(properties);
		return tag;
	}

	private static Property property(Property.Type type, Consumer<Property> setup) {
		Property property = new Property();
		property.setType(type);
		setup.accept(property);
		return property;
	}
}
