package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.Problem;
import com.gentics.contentnode.mcp.util.ExternalFormException;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.ItemVersion;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.formtype.BlockConfig;
import com.gentics.contentnode.rest.model.formtype.Blocks;
import com.gentics.contentnode.rest.model.formtype.ControlConfig;
import com.gentics.contentnode.rest.model.formtype.Controls;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationModel;
import com.gentics.contentnode.rest.model.formtype.OptionsSetting;
import com.gentics.contentnode.rest.model.formtype.SelectOption;
import com.gentics.contentnode.rest.model.formtype.SelectSetting;

/**
 * Unit tests for the Forms tools. Everything that needs a real form, and the permissions, is covered by the live check.
 */
public class FormToolsTest {
	private static final ObjectRef REF = ObjectRef.of(ObjectRef.Type.FORM, 20);

	@Test
	public void testDefinitions() {
		assertDefinition(new GetFormTool(), "get_form", "Get form", "formId");
		assertDefinition(new ListFormTypesTool(), "list_form_types", "List form types", "nodeId");
		assertDefinition(new GetFormTypeTool(), "get_form_type", "Get form type", "nodeId", "type");
		assertDefinition(new CreateFormTool(), "create_form", "Create form", "name", "formType", "languages");
		assertDefinition(new UpdateFormTool(), "update_form", "Update form", "formId");
		assertDefinition(new DeleteFormTool(), "delete_form", "Delete form to wastebin", "formId");
		assertDefinition(new PublishFormTool(), "publish_form", "Publish form", "formId");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		for (Call call : List.of(new Call(new GetFormTool(), Map.of("formId", 20)),
				new Call(new ListFormTypesTool(), Map.of("nodeId", 1)),
				new Call(new GetFormTypeTool(), Map.of("nodeId", 1, "type", "generic")),
				new Call(new CreateFormTool(),
						Map.of("name", "n", "formType", "t", "languages", List.of("de"))),
				new Call(new UpdateFormTool(), Map.of("formId", 20, "name", "n")),
				new Call(new DeleteFormTool(), Map.of("formId", 20)),
				new Call(new PublishFormTool(), Map.of("formId", 20)))) {
			assertRejectsWithoutCredentials(call.tool(), call.arguments());
		}
	}

	@Test
	public void testInputValidation() {
		assertAccepted(new CreateFormTool(), Map.of("name", "n", "formType", "t", "languages", List.of("de"), "data",
				Map.of("a", 1)));
		assertRejected(new CreateFormTool(), Map.of("name", "n", "formType", "t"));
		assertRejected(new CreateFormTool(),
				Map.of("name", "n", "formType", "t", "languages", List.of("de"), "externalId", "x"));
		assertRejected(new DeleteFormTool(), Map.of("formId", 20, "purge", true));
	}

	@Test
	public void testUpdateNeedsAChange() {
		assertThatThrownBy(() -> new UpdateFormTool().invoke(Map.of("formId", 20), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Nothing to change");
	}

	@Test
	public void testModelCopiesWhatIsGiven() {
		FormModel form = Forms.model(Map.of("name", "Feedback", "languages", List.of("de", "en"), "data",
				Map.of("key", "value")));
		assertThat(form.getName()).isEqualTo("Feedback");
		assertThat(form.getLanguages()).containsExactly("de", "en");
		assertThat(form.getData().get("key").asText()).isEqualTo("value");
		assertThat(form.getDescription()).isNull();
		assertThat(UpdateFormTool.changed(Map.of("name", "Feedback", "nodeId", 1))).containsExactly("name");
		assertThatThrownBy(() -> Forms.model(Map.of("languages", List.of())))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testExternalProblemType() {
		Problem problem = Problem.of("update_form", new ExternalFormException("external"));
		assertThat(problem.type()).endsWith("/external-form-read-only");
		assertThat(problem.status()).isEqualTo(422);
	}

	@Test
	public void testResults() {
		Map<String, Object> form = assertValidOutput(new GetFormTool(), new GetFormTool.Result(REF, "n", null, "generic",
				true, List.of("de"), false, false, false, Map.of("a", 1), new Forms.Version("0.2", 1700000000)));
		assertThat(form).containsEntry("external", true).containsKey("data").containsEntry("version",
				Map.of("number", "0.2", "timestamp", 1700000000));
		assertValidOutput(new ListFormTypesTool(),
				new ListFormTypesTool.Result(List.of(new ListFormTypesTool.Item("generic", "Generic", false))));
		assertValidOutput(new CreateFormTool(), new CreateFormTool.Result(REF, new Forms.Version("0.1", 1)));
		assertValidOutput(new CreateFormTool(), new CreateFormTool.Result(REF, null));
		assertValidOutput(new UpdateFormTool(),
				new UpdateFormTool.Result(REF, List.of("name"), new Forms.Version("0.2", 2)));
		assertValidOutput(new DeleteFormTool(), new DeleteFormTool.Result(REF, true, true));
		assertValidOutput(new PublishFormTool(), new PublishFormTool.Result(REF, true));
	}

	@Test
	public void testVersion() {
		FormModel form = new FormModel();
		assertThat(Forms.version(form)).isNull();
		form.setVersion(new ItemVersion().setNumber("1.0").setTimestamp(42));
		assertThat(Forms.version(form)).isEqualTo(new Forms.Version("1.0", 42));
	}

	@Test
	public void testGetFormType() throws Exception {
		FormTypeConfigurationModel type = type();
		GetFormTypeTool.requireOffered(List.of(type), "generic", 1);
		assertThatThrownBy(() -> GetFormTypeTool.requireOffered(List.of(type), "poll", 1))
				.isInstanceOf(EntityNotFoundException.class);
		assertThat(Problem.of("get_form_type", new EntityNotFoundException("x")).type()).endsWith("/not-found");

		Map<String, Object> result = assertValidOutput(new GetFormTypeTool(), GetFormTypeTool.result(type));
		assertThat(result).containsEntry("type", "generic").containsKey("controls").doesNotContainKey("pluginName");
	}

	@Test
	public void testPlaceholdersAreNamed() throws Exception {
		JsonNode data = FormDefinitions.prepare(json("""
				{"schema": {"properties": {"new:q": {"name": "new:q", "type": "string"}}},
				 "ui-schema": {"pages": [{"pagename": {"de": "Allgemein", "en": "General"}, "elements": [
				   {"type": "container", "id": "new:box", "formGridOptions": {"type": "box"}, "elements": [
				     {"type": "property", "id": "new:q", "label": {"de": "Frage", "en": "Question"},
				      "formGridOptions": {"type": "string", "showIf": "new:q"}}]}]}]}}
				"""), type(), List.of("de", "en"));

		String name = data.path("schema").path("properties").fieldNames().next();
		assertThat(name).matches("string_[0-9a-f]{32}");
		assertThat(data.at("/schema/properties/" + name + "/name").asText()).isEqualTo(name);
		JsonNode box = data.at("/ui-schema/pages/0/elements/0");
		assertThat(box.path("id").asText()).matches("box_[0-9a-f]{32}");
		assertThat(box.at("/elements/0/id").asText()).isEqualTo(name);
		assertThat(box.at("/elements/0/formGridOptions/showIf").asText()).isEqualTo(name);
		assertThat(data.at("/schema/key").asText()).isNotBlank().isEqualTo(data.at("/ui-schema/key").asText());
		assertThat(UUID.fromString(data.at("/schema/key").asText())).isNotNull();
		assertThat(data.at("/schema/version").asText()).isEqualTo("1.0");
		assertThat(data.at("/ui-schema/version").asText()).isEqualTo("1.0");
	}

	@Test
	public void testExistingNamesAreKept() throws Exception {
		JsonNode given = json("""
				{"flowId": "default",
				 "schema": {"key": "k", "version": "1.0", "properties": {"string_1": {"name": "string_1", "type": "string"},
				   "new:c": {"name": "new:c", "type": "choice",
				     "validation": {"regexValidation": {"key": "email", "regex": ".+@.+"}},
				     "formGridOptions": {"selectOptions": [{"value": "a", "label": {"de": "A"}}]}}}},
				 "ui-schema": {"key": "k", "version": "1.0", "pages": [{"pagename": {"de": "Seite"}, "elements": [
				   {"type": "property", "id": "string_1", "label": {"de": "Frage"}, "formGridOptions": {"type": "string"}},
				   {"type": "property", "id": "new:c", "formGridOptions": {"type": "choice", "size": "small"}}]}]}}
				""");
		JsonNode data = FormDefinitions.prepare(given, type(), List.of("de"));
		assertThat(data.at("/ui-schema/pages/0/elements/1/formGridOptions/size").asText()).isEqualTo("small");

		assertThat(data.at("/schema/properties/string_1/name").asText()).isEqualTo("string_1");
		assertThat(data.at("/ui-schema/pages/0/elements/0/id").asText()).isEqualTo("string_1");
		assertThat(data.at("/ui-schema/pages/0/elements/1/id").asText()).matches("choice_[0-9a-f]{32}");
		assertThat(data.at("/schema/key").asText()).isEqualTo("k");
		assertThat(data.path("flowId").asText()).isEqualTo("default");
	}

	@Test
	public void testRefusesWhatDoesNotFit() throws Exception {
		JsonNode given = json("""
				{"schema": {"properties": {
				   "string_1": {"name": "string_1", "type": "string"},
				   "orphan_1": {"name": "orphan_1", "type": "string"},
				   "odd_1": {"name": "odd_1", "type": "slider"},
				   "choice_1": {"name": "choice_1", "type": "choice", "size": "small",
				     "validation": {"regexValidation": {"key": "email", "regex": ".*"}},
				     "selectOptions": [{"key": "a", "value": "A"}]},
				   "choice_2": {"name": "choice_2", "type": "choice",
				     "formGridOptions": {"selectOptions": [{"key": "a"}, {"value": "b", "label": {"de": "B"}}]}},
				   "new:lost": {"name": "new:lost", "type": "string"}}},
				 "ui-schema": {"pages": [{"pagename": {"de": "Allgemein"}, "elements": [
				   {"type": "property", "id": "string_1", "label": {"de": "Frage"}, "formGridOptions": {"type": "string"}},
				   {"type": "property", "id": "missing_1", "formGridOptions": {"type": "string"}},
				   {"type": "property", "id": "odd_1", "formGridOptions": {"type": "slider"}},
				   {"type": "property", "id": "choice_1", "formGridOptions": {"type": "choice", "size": "huge"}},
				   {"type": "property", "id": "new:untyped"}]},
				   {"Request": {"de": "Anfrage"}, "elements": [
				   {"type": "property", "id": "choice_2", "formGridOptions": {"type": "choice"}}]}]}}
				""");

		assertThatThrownBy(() -> FormDefinitions.prepare(given, type(), List.of("de", "en")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("element 'new:untyped' has no formGridOptions.type")
				.hasMessageContaining("schema property 'new:lost' has no ui-schema element telling its type")
				.hasMessageContaining("schema property 'odd_1' has type 'slider', which is not a control")
				.hasMessageContaining("element 'odd_1' has formGridOptions.type 'slider', which is neither")
				.hasMessageContaining("element 'missing_1' is a control without a schema property")
				.hasMessageContaining("schema property 'orphan_1' has no ui-schema control element")
				.hasMessageContaining("element 'string_1': label has no text for language 'en'")
				.hasMessageContaining("page 0: pagename has no text for language 'en'")
				.hasMessageContaining("element 'choice_1': setting 'size' has the value \"huge\", which is not one")
				.hasMessageContaining("element 'choice_1': setting 'validation' has the value")
				.hasMessageContaining("element 'choice_1': setting 'size' is at the schema property.size, but belongs at "
						+ "the ui-schema element.formGridOptions.size")
				.hasMessageContaining("element 'choice_1': setting 'selectOptions' is at the schema property.selectOptions, "
						+ "but belongs at the schema property.formGridOptions.selectOptions")
				.hasMessageContaining("element 'choice_2': setting 'selectOptions', option 0 needs a value")
				.hasMessageContaining("element 'choice_2': setting 'selectOptions', option 0 needs a label")
				.hasMessageContaining("element 'choice_2': setting 'selectOptions', option 1: label has no text for "
						+ "language 'en'")
				.hasMessageContaining("page 1 has no pagename")
				.satisfies(e -> assertThat(Problem.of("create_form", e).type()).endsWith("/invalid-data"));
		assertThatThrownBy(() -> FormDefinitions.prepare(json("{\"schema\": {}}"), type(), List.of("de")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("schema and ui-schema");
	}

	/**
	 * Build a form type with the controls string and choice (the frontend select setting size, kept in the ui-schema
	 * element's formGridOptions; the backend select setting validation, kept in the schema property at
	 * validation.regexValidation with object values; the backend options setting selectOptions, kept in the schema
	 * property's formGridOptions) and the container block box
	 * @return form type
	 */
	private static FormTypeConfigurationModel type() {
		ControlConfig choice = new ControlConfig(Map.of("en", "Choice"));
		SelectSetting validation = new SelectSetting("validation", Map.of("en", "Validation"), List.of(
				new SelectOption(Map.of("key", "email", "regex", ".+@.+"), Map.of("en", "E-mail"))));
		validation.propertyPath = "validation.regexValidation";
		validation.backend = true;
		OptionsSetting options = new OptionsSetting("selectOptions", Map.of("en", "Options"));
		options.backend = true;
		choice.settings = List.of(new SelectSetting("size", Map.of("en", "Size"),
				List.of(new SelectOption("small", Map.of("en", "Small")), new SelectOption("large", Map.of("en", "Large")))),
				validation, options);
		BlockConfig box = new BlockConfig(Map.of("en", "Box"));
		box.container = true;

		FormTypeConfigurationModel type = new FormTypeConfigurationModel();
		type.type = "generic";
		type.nameI18n = Map.of("en", "Generic");
		type.controls = new Controls(Map.of("string", new ControlConfig(Map.of("en", "Text")), "choice", choice));
		type.blocks = new Blocks(Map.of("box", box));
		return type;
	}

	private static JsonNode json(String json) throws Exception {
		return new ObjectMapper().readTree(json);
	}

	private record Call(AbstractMcpTool tool, Map<String, Object> arguments) {
	}
}
