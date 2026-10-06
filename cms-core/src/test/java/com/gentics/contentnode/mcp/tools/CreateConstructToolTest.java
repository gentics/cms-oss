package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.tools.CreateConstructTool.Request;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.EditorControlStyle;
import com.gentics.contentnode.rest.model.Part;

/**
 * Unit tests for {@link CreateConstructTool}. Creating, keyword collisions and permissions are covered by the live
 * check.
 */
public class CreateConstructToolTest {
	private final CreateConstructTool tool = new CreateConstructTool();

	private static final List<String> UI_LANGUAGES = List.of("de", "en");

	@Test
	public void testDefinition() {
		assertDefinition(tool, "create_construct", "Create construct", "keyword", "name", "nodeIds",
				"handlebarsTemplate");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, minimal());
		assertAccepted(tool, with(Map.of("parts", List.of(Map.of("keyword", "choice", "name", Map.of("en", "C"),
				"typeId", 29, "datasourceId", 7)), "allowExisting", true, "editorControlStyle", "CLICK")));
		assertRejected(tool, with(Map.of("keyword", "Teaser")));
		assertRejected(tool, with(Map.of("nodeIds", List.of())));
		assertRejected(tool, with(Map.of("parts", List.of(Map.of("keyword", "choice", "name", Map.of("en", "C"),
				"typeId", 29)))));
		assertRejected(tool, with(Map.of("parts", List.of(Map.of("keyword", "text", "name", Map.of("en", "T"),
				"typeId", 1, "datasourceId", 7)))));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, minimal());
	}

	@Test
	public void testRequest() {
		Request request = Request.of(with(Map.of("parts", List.of(Map.of("keyword", "headline", "name", Map.of("en",
				"Headline"), "typeId", 1)), "categoryId", 7)), UI_LANGUAGES);

		Construct construct = request.construct();
		assertThat(construct.getKeyword()).isEqualTo("genaix_teaser");
		assertThat(construct.getCategoryId()).isEqualTo(7);
		assertThat(construct.getMayBeSubtag()).isTrue();
		assertThat(construct.getMayContainSubtags()).isFalse();
		assertThat(construct.getAutoEnable()).isTrue();
		assertThat(construct.getEditorControlStyle()).isEqualTo(EditorControlStyle.ABOVE);
		assertThat(construct.getParts()).extracting(Part::getKeyword).containsExactly("handlebars", "headline");
		assertThat(construct.getParts()).extracting(Part::getPartOrder).containsExactly(1, 2);
		assertThat(construct.getParts().get(0).getDefaultProperty().getStringValue())
				.isEqualTo("<b>{{cms.tag.parts.headline}}</b>");
		assertThat(request.templateKeyword()).isEqualTo("handlebars");
		assertThat(request.nodeIds()).containsExactly(1);
		assertThat(request.allowExisting()).isFalse();
	}

	@Test
	public void testRequestRejections() {
		assertRequestRejected(with(Map.of("keyword", "k".repeat(65))), "at most 64");
		assertRequestRejected(with(Map.of("visibleInMenu", false)), "'visibleInMenu' cannot be false");
		assertRequestRejected(with(Map.of("name", Map.of("fr", "Accroche"))), "UI languages");
		assertRequestRejected(with(Map.of("templatePartKeyword", "Tpl")), "templatePartKeyword");
		assertRequestRejected(with(Map.of("editorControlStyle", "LEFT")), "ASIDE, ABOVE or CLICK");
		Map<String, Object> noNodes = minimal();
		noNodes.remove("nodeIds");
		assertRequestRejected(noNodes, "Missing required argument 'nodeIds'");
	}

	@Test
	public void testResult() {
		Construct construct = Request.of(minimal(), UI_LANGUAGES).construct().setId(12).setName("Teaser");

		CreateConstructTool.Result result = CreateConstructTool.result(construct, true, List.of(ObjectRef.of(
				ObjectRef.Type.NODE, 1)));

		assertThat(result.templatePart()).isEqualTo(new CreateConstructTool.TemplatePart("handlebars", 43,
				"RICHTEXT"));
		assertThat(result.construct().template().source()).isEqualTo("<b>{{cms.tag.parts.headline}}</b>");
		assertValidOutput(tool, result);
	}

	private static Map<String, Object> minimal() {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("keyword", "genaix_teaser");
		arguments.put("name", Map.of("de", "Teaser", "en", "Teaser"));
		arguments.put("nodeIds", List.of(1));
		arguments.put("handlebarsTemplate", "<b>{{cms.tag.parts.headline}}</b>");
		return arguments;
	}

	private static Map<String, Object> with(Map<String, Object> changes) {
		Map<String, Object> arguments = minimal();
		arguments.putAll(changes);
		return arguments;
	}

	private static void assertRequestRejected(Map<String, Object> arguments, String message) {
		assertThatThrownBy(() -> Request.of(arguments, UI_LANGUAGES)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(message);
	}
}
