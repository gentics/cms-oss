package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.rest.model.MarkupLanguage;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.TemplateInNode;

/**
 * Unit tests for {@link ListTemplatesTool}. The three scopes are covered by the live check.
 */
public class ListTemplatesToolTest {
	private final ListTemplatesTool tool = new ListTemplatesTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "list_templates", "List templates");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of());
		assertAccepted(tool, Map.of("nodeId", 1, "folderId", 57, "q", "article", "size", 10));
		assertRejected(tool, Map.of("folderId", 0));
		assertRejected(tool, Map.of("includeSource", true));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of());
	}

	@Test
	public void testDuplicatesRemovedAndNodeKept() {
		TemplateInNode first = inNode(9, "Article", 1);
		TemplateInNode again = inNode(9, "Article", 2);
		TemplateInNode other = inNode(10, "Home", 2);

		ListResult<ListTemplatesTool.TemplateSummary> result = ListTemplatesTool.result(List.of(first, again, other),
				null, ListArgs.of(Map.of(), ListTemplatesTool.LIMITS));

		assertThat(result.total()).isEqualTo(2);
		assertThat(result.items()).extracting(item -> item.ref().nodeId()).containsExactly(1, 2);
		assertThat(result.items().get(0).markupLanguage()).isEqualTo("HTML");
		assertValidOutput(tool, result);
	}

	@Test
	public void testSlice() {
		Template template = new Template();
		template.setId(9);
		template.setName("Article");

		ListResult<ListTemplatesTool.TemplateSummary> result = ListTemplatesTool.result(List.of(template, inNode(10,
				"Home", 1)), 3, ListArgs.of(Map.of("size", 1), ListTemplatesTool.LIMITS));

		assertThat(result.items()).extracting(item -> item.ref().id()).containsExactly(9);
		assertThat(result.items().get(0).ref().nodeId()).isEqualTo(3);
		assertThat(result.truncated()).isTrue();
	}

	private static TemplateInNode inNode(int id, String name, int nodeId) {
		MarkupLanguage html = new MarkupLanguage();
		html.setName("HTML");
		TemplateInNode template = new TemplateInNode();
		template.setId(id);
		template.setName(name);
		template.setNodeId(nodeId);
		template.setMarkupLanguage(html);
		return template;
	}
}
