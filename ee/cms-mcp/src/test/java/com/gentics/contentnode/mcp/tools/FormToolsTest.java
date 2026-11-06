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

import org.junit.Test;

import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.Problem;
import com.gentics.contentnode.mcp.util.ExternalFormException;
import com.gentics.contentnode.rest.model.form.FormModel;

/**
 * Unit tests for the Forms tools. Everything that needs a real form, and the permissions, is covered by the live check.
 */
public class FormToolsTest {
	private static final ObjectRef REF = ObjectRef.of(ObjectRef.Type.FORM, 20);

	@Test
	public void testDefinitions() {
		assertDefinition(new GetFormTool(), "get_form", "Get form", "formId");
		assertDefinition(new ListFormTypesTool(), "list_form_types", "List form types", "nodeId");
		assertDefinition(new CreateFormTool(), "create_form", "Create form", "name", "formType", "languages");
		assertDefinition(new UpdateFormTool(), "update_form", "Update form", "formId");
		assertDefinition(new DeleteFormTool(), "delete_form", "Delete form to wastebin", "formId");
		assertDefinition(new PublishFormTool(), "publish_form", "Publish form", "formId");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		for (Call call : List.of(new Call(new GetFormTool(), Map.of("formId", 20)),
				new Call(new ListFormTypesTool(), Map.of("nodeId", 1)),
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
				true, List.of("de"), false, false, false, Map.of("a", 1)));
		assertThat(form).containsEntry("external", true).containsKey("data");
		assertValidOutput(new ListFormTypesTool(),
				new ListFormTypesTool.Result(List.of(new ListFormTypesTool.Item("generic", "Generic", false))));
		assertValidOutput(new CreateFormTool(), new CreateFormTool.Result(REF));
		assertValidOutput(new UpdateFormTool(), new UpdateFormTool.Result(REF, List.of("name")));
		assertValidOutput(new DeleteFormTool(), new DeleteFormTool.Result(REF, true, true));
		assertValidOutput(new PublishFormTool(), new PublishFormTool.Result(REF, true));
	}

	private record Call(AbstractMcpTool tool, Map<String, Object> arguments) {
	}
}
