package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.TagInfo;
import com.gentics.contentnode.rest.model.NodeIdObjectId;
import com.gentics.contentnode.rest.model.Overview;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.SelectOption;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;

/**
 * Unit tests for {@link PartValues}: every writable part type takes the value get_page_tags reads, fits the schema
 * get_construct publishes for it, and reads back unchanged.
 */
public class PartValuesTest {
	private static final DefaultJsonSchemaValidator VALIDATOR = new DefaultJsonSchemaValidator();

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	public void testRoundTrip() {
		assertRoundTrip(Property.Type.STRING, "Hello");
		assertRoundTrip(Property.Type.RICHTEXT, "<p>Before</p><node manimage1><p>After</p>");
		assertRoundTrip(Property.Type.BOOLEAN, true);
		assertRoundTrip(Property.Type.FORM, "0ab1-uuid");
		assertRoundTrip(Property.Type.PAGE, "https://example.com/");
		assertRoundTrip(Property.Type.PAGE, "");
		assertRoundTrip(Property.Type.LIST, List.of("one", "two"));
		assertRoundTrip(Property.Type.ORDEREDLIST, List.of("one"));
		assertRoundTrip(Property.Type.UNORDEREDLIST, List.of());
		assertRoundTrip(Property.Type.DATASOURCE, List.of(Map.of("key", "q1", "value", "Answer")));
		assertRoundTrip(Property.Type.PAGETAG, Map.of("pageId", 4711, "contentTagId", 12));
		assertRoundTrip(Property.Type.TEMPLATETAG, Map.of("templateId", 3, "templateTagId", 9));
		Property select = selectProperty(Property.Type.SELECT);
		PartValues.write("t", "p", select, "Blue");
		assertThat(TagInfo.value(select)).isEqualTo("Blue");
		assertFitsSchema(select, "Blue");
		Property multi = selectProperty(Property.Type.MULTISELECT);
		PartValues.write("t", "p", multi, List.of("Red", "Blue"));
		assertThat(TagInfo.value(multi)).isEqualTo(List.of("Red", "Blue"));
		assertFitsSchema(multi, List.of("Red", "Blue"));
	}

	@Test
	public void testReferences() {
		for (Property.Type type : List.of(Property.Type.IMAGE, Property.Type.FILE, Property.Type.FOLDER,
				Property.Type.NODE, Property.Type.CMSFORM, Property.Type.PAGE)) {
			ObjectRef.Type refType = type == Property.Type.CMSFORM ? ObjectRef.Type.FORM
					: ObjectRef.Type.valueOf(type.name());
			Property byId = property(type);
			PartValues.write("t", "p", byId, 567);
			assertThat(TagInfo.value(byId)).as(type.name()).isEqualTo(ObjectRef.of(refType, 567));
			assertFitsSchema(byId, 567);

			// the ref get_page_tags returns is accepted back unchanged
			Map<String, Object> ref = Map.of("type", refType.value(), "id", 567, "nodeId", 2, "name", "x.jpg");
			Property byRef = property(type);
			PartValues.write("t", "p", byRef, ref);
			assertThat(TagInfo.value(byRef)).isEqualTo(ObjectRef.of(refType, 567));
			assertFitsSchema(byRef, ref);
		}
		Property page = property(Property.Type.PAGE);
		PartValues.write("t", "p", page, Map.of("type", "page", "id", 7, "nodeId", 2));
		assertThat(page.getNodeId()).isEqualTo(2);
	}

	@Test
	public void testOverview() {
		Overview existing = new Overview();
		existing.setListType(Overview.ListType.IMAGE);
		existing.setSelectType(Overview.SelectType.MANUAL);
		existing.setOrderBy(Overview.OrderBy.SELF);
		existing.setMaxItems(5);
		existing.setSelectedItemIds(List.of(1));
		Property property = property(Property.Type.OVERVIEW);
		property.setOverview(existing);

		Map<String, Object> value = Map.of("items", List.of(567, Map.of("type", "image", "id", 568)),
				"orderDirection", "DESC");
		PartValues.write("t", "p", property, value);
		assertFitsSchema(property, value);

		@SuppressWarnings("unchecked")
		Map<String, Object> read = (Map<String, Object>) TagInfo.value(property);
		assertThat(read).containsEntry("listType", "IMAGE").containsEntry("selectType", "MANUAL")
				.containsEntry("orderBy", "SELF").containsEntry("orderDirection", "DESC").containsEntry("maxItems", 5);
		assertThat(read.get("items")).isEqualTo(List.of(ObjectRef.of(ObjectRef.Type.IMAGE, 567),
				new ObjectRef(ObjectRef.Type.IMAGE, 568, null, null, null, null, null, null, null)));
		assertThat(property.getOverview().getSelectedNodeItemIds()).extracting(NodeIdObjectId::getObjectId)
				.containsExactly(567, 568);

		// what was read is written back unchanged
		Property again = property(Property.Type.OVERVIEW);
		PartValues.write("t", "p", again, MAPPER.convertValue(read, Map.class));
		assertThat(PartValues.same(again, property)).isTrue();
	}

	@Test
	public void testDatasourceKeepsEntryIds() {
		Property property = property(Property.Type.DATASOURCE);
		property.setOptions(new ArrayList<>(List.of(option(4, "q1", "Old"))));
		PartValues.write("t", "p", property,
				List.of(Map.of("key", "q2", "value", "New"), Map.of("key", "q1", "value", "Changed")));
		assertThat(property.getOptions()).extracting(SelectOption::getId).containsExactly(null, 4);
	}

	@Test
	public void testSelectByKey() {
		Property select = selectProperty(Property.Type.SELECT);
		PartValues.write("t", "p", select, "b");
		assertThat(TagInfo.value(select)).isEqualTo("Blue");
		PartValues.write("t", "p", select, null);
		assertThat(TagInfo.value(select)).isNull();
	}

	@Test
	public void testRejections() {
		assertRejected(Property.Type.STRING, 5, "needs a string");
		assertRejected(Property.Type.BOOLEAN, "yes", "needs a boolean");
		assertRejected(Property.Type.IMAGE, 0, "positive integer ID");
		assertRejected(Property.Type.IMAGE, Map.of("type", "file", "id", 3), "ref of type image");
		assertRejected(Property.Type.PAGE, 4.5, "positive integer ID");
		assertRejected(Property.Type.LIST, List.of("a", 1), "array of strings");
		assertRejected(Property.Type.DATASOURCE, List.of(Map.of("key", "a")), "{key, value}");
		assertRejected(Property.Type.OVERVIEW, Map.of("listType", "THING"), "listType");
		assertRejected(Property.Type.OVERVIEW, Map.of("source", "x"), "members");
		assertRejected(Property.Type.PAGETAG, Map.of("pageId", 1), "contentTagId");
		assertRejected(Property.Type.VELOCITY, "x", "cannot be written");
		assertThatThrownBy(() -> PartValues.write("t", "p", selectProperty(Property.Type.SELECT), "Green"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no option 'Green'")
				.hasMessageContaining("Red, Blue");
	}

	@Test
	public void testValueSchema() {
		for (Property.Type type : Property.Type.values()) {
			assertThat(PartValues.valueSchema(type, null) != null).as(type.name())
					.isEqualTo(PartValues.WRITABLE.contains(type));
		}
		assertThat(PartValues.valueSchema(Property.Type.SELECT, List.of("Red", "Blue")))
				.containsEntry("enum", List.of("Red", "Blue"));
	}

	private static void assertRoundTrip(Property.Type type, Object value) {
		Property property = property(type);
		PartValues.write("t", "p", property, value);
		assertThat(TagInfo.value(property)).as(type.name()).isEqualTo(value);
		assertFitsSchema(property, value);
	}

	private static void assertFitsSchema(Property property, Object value) {
		List<String> options = property.getOptions() != null
				? property.getOptions().stream().map(SelectOption::getValue).toList()
				: null;
		Map<String, Object> schema = PartValues.valueSchema(property.getType(), options);
		Map<String, Object> wrapper = new LinkedHashMap<>();
		wrapper.put("type", "object");
		wrapper.put("properties", Map.of("v", schema));
		ValidationResponse validation = VALIDATOR.validate(wrapper, Map.of("v", MAPPER.convertValue(value,
				Object.class)));
		assertThat(validation.valid()).as(property.getType() + ": " + validation.errorMessage()).isTrue();
	}

	private static void assertRejected(Property.Type type, Object value, String message) {
		assertThatThrownBy(() -> PartValues.write("tag1", "part1", property(type), value))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tag1")
				.hasMessageContaining(message);
	}

	private static Property property(Property.Type type) {
		Property property = new Property();
		property.setType(type);
		return property;
	}

	private static Property selectProperty(Property.Type type) {
		Property property = property(type);
		property.setOptions(List.of(option(1, "r", "Red"), option(2, "b", "Blue")));
		return property;
	}

	private static SelectOption option(Integer id, String key, String value) {
		SelectOption option = new SelectOption();
		option.setId(id);
		option.setKey(key);
		option.setValue(value);
		return option;
	}
}
