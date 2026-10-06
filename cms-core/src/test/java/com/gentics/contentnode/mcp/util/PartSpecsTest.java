package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.util.PartSpecs.PartSpec;
import com.gentics.contentnode.rest.model.Part;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.SelectSetting;

import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;

/**
 * Unit tests for {@link PartSpecs}
 */
public class PartSpecsTest {
	private static final List<String> UI_LANGUAGES = List.of("de", "en");

	private static final Map<String, String> NAME = Map.of("en", "Headline");

	@Test
	public void testParse() {
		Map<String, Object> raw = Map.of("keyword", "headline", "name", NAME, "typeId", 1, "defaultValue", "Hi");

		assertThat(PartSpecs.parse(Map.of("parts", List.of(raw)), UI_LANGUAGES))
				.containsExactly(new PartSpec("headline", NAME, 1, null, true, false, false, false, null, "Hi"));
		assertThat(PartSpecs.parse(Map.of(), UI_LANGUAGES)).isNull();
	}

	@Test
	public void testParseRejections() {
		assertParseRejected(Map.of("keyword", "Headline", "name", NAME, "typeId", 1), "parts[0]: Argument 'keyword'");
		assertParseRejected(Map.of("name", NAME, "typeId", 1), "Missing required argument 'keyword'");
		assertParseRejected(Map.of("keyword", "a", "typeId", 1), "Missing required argument 'name'");
		assertParseRejected(Map.of("keyword", "a", "name", Map.of("fr", "Titre"), "typeId", 1), "UI languages");
		assertParseRejected(Map.of("keyword", "a", "name", NAME), "Missing required argument 'typeId'");
		assertParseRejected(Map.of("keyword", "a", "name", NAME, "typeId", 1, "x", 1), "Unknown member 'x'");
		assertParseRejected(Map.of("keyword", "a", "name", NAME, "typeId", 1, "defaultValue", 3), "string or boolean");
	}

	@Test
	public void testValidate() {
		PartSpecs.validate(List.of(part("text", 1, null, null), part("choice", 29, 7, null),
				part("flag", 31, null, true)), "handlebars", id -> id == 7);
	}

	@Test
	public void testValidateRejections() {
		assertInvalid(List.of(part("tpl", 43, null, null)), "type 43 is the Handlebars template");
		assertInvalid(List.of(part("overview", 13, null, null)), "type 13 is not allowed");
		assertInvalid(List.of(part("handlebars", 1, null, null)), "keyword is the one of the Handlebars part");
		assertInvalid(List.of(part("text", 1, null, null), part("text", 2, null, null)), "more than once");
		assertInvalid(List.of(part("choice", 29, null, null)), "needs a datasourceId");
		assertInvalid(List.of(part("choice", 30, 8, null)), "datasource 8 does not exist");
		assertInvalid(List.of(part("text", 1, 7, null)), "only select parts");
		assertInvalid(List.of(part("text", 1, null, true)), "default value");
		assertInvalid(List.of(part("flag", 31, null, "yes")), "default value");
	}

	@Test
	public void testCheckKeyword() {
		PartSpecs.checkKeyword("keyword", "genaix_teaser", 64);
		assertThatThrownBy(() -> PartSpecs.checkKeyword("keyword", "a".repeat(65), 64))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at most 64");
		assertThatThrownBy(() -> PartSpecs.checkKeyword("keyword", "1abc", 64))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'1abc'");
	}

	@Test
	public void testToRest() {
		Part text = PartSpecs.toRest(part("text", 1, null, "Hi"), 2, null);
		assertThat(text.getKeyword()).isEqualTo("text");
		assertThat(text.getNameI18n()).isEqualTo(NAME);
		assertThat(text.getTypeId()).isEqualTo(1);
		assertThat(text.isEditable()).isTrue();
		assertThat(text.getPartOrder()).isEqualTo(2);
		assertThat(text.getGlobalId()).isNull();
		assertThat(text.getDefaultProperty().getType()).isEqualTo(Property.Type.STRING);
		assertThat(text.getDefaultProperty().getStringValue()).isEqualTo("Hi");

		Part flag = PartSpecs.toRest(part("flag", 31, null, true), 3, null);
		assertThat(flag.getDefaultProperty().getBooleanValue()).isTrue();

		Part existing = new Part().setGlobalId("A547.1").setSelectSettings(new SelectSetting());
		Part select = PartSpecs.toRest(new PartSpec("choice", NAME, 29, 7, false, false, true, false, 5, null), 4,
				existing);
		assertThat(select).isSameAs(existing);
		assertThat(select.getGlobalId()).isEqualTo("A547.1");
		assertThat(select.getSelectSettings().getDatasourceId()).isEqualTo(7);
		assertThat(select.getPartOrder()).isEqualTo(5);
		assertThat(select.isMandatory()).isTrue();
		assertThat(select.isEditable()).isFalse();
	}

	@Test
	public void testTemplatePart() {
		Part template = PartSpecs.templatePart("handlebars", "<b>{{cms.tag.parts.text}}</b>", null);

		assertThat(template.getTypeId()).isEqualTo(43);
		assertThat(template.getPartOrder()).isEqualTo(1);
		assertThat(template.isEditable()).isFalse();
		assertThat(template.getDefaultProperty().getType()).isEqualTo(Property.Type.RICHTEXT);
		assertThat(template.getDefaultProperty().getStringValue()).isEqualTo("<b>{{cms.tag.parts.text}}</b>");
		assertThat(PartSpecs.templateOf(List.of(new Part().setTypeId(1), template))).isSameAs(template);
		assertThat(PartSpecs.templateOf(null)).isNull();
	}

	@Test
	public void testJsonSchema() {
		Map<String, Object> schema = new HashMap<>(Map.of("type", "object", "properties",
				Map.of("parts", PartSpecs.jsonSchema("Parts."))));
		assertThat(new DefaultJsonSchemaValidator().validateSchema(schema).valid()).isTrue();
	}

	private static PartSpec part(String keyword, int typeId, Integer datasourceId, Object defaultValue) {
		return new PartSpec(keyword, NAME, typeId, datasourceId, true, false, false, false, null, defaultValue);
	}

	private static void assertParseRejected(Map<String, Object> part, String message) {
		assertThatThrownBy(() -> PartSpecs.parse(Map.of("parts", List.of(part)), UI_LANGUAGES))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
	}

	private static void assertInvalid(List<PartSpec> parts, String message) {
		assertThatThrownBy(() -> PartSpecs.validate(parts, "handlebars", id -> id == 7))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
	}
}
