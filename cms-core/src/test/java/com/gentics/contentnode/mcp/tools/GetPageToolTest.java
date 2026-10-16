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

import org.junit.Test;

import com.gentics.contentnode.mcp.util.Args;

/**
 * Unit tests for {@link GetPageTool}. Loading a real page is covered by the live check.
 */
public class GetPageToolTest {
	private final GetPageTool tool = new GetPageTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_page", "Get page", "id");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", 4711, "include", List.of("tags", "template", "constructs")));
		assertRejected(tool, Map.of("id", 4711, "include", List.of("source")));
		assertRejected(tool, Map.of("id", 4711, "include", List.of("tags", "tags")));
		assertRejected(tool, Map.of("id", 4711, "update", true));
	}

	@Test
	public void testIncludeArgument() {
		assertThat(Args.enumList(Map.of(), GetPageTool.ARG_INCLUDE, GetPageTool.INCLUDES, List.of("tags")))
				.containsExactly("tags");
		assertThatThrownBy(() -> Args.enumList(Map.of("include", List.of("source")), GetPageTool.ARG_INCLUDE,
				GetPageTool.INCLUDES, List.of())).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
						"source");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", 4711));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDefaultResult() {
		GetPageTool.Result result = GetPageTool.result(PageFixtures.page(), List.of("tags"),
				Map.of(8, "text", 5, "richtext"));

		Map<String, Object> json = assertValidOutput(tool, result);
		assertThat(json).containsOnlyKeys("page");
		Map<String, Object> page = (Map<String, Object>) json.get("page");
		assertThat((Map<String, Object>) page.get("ref")).containsEntry("id", 4711).containsEntry("nodeId", 3);
		assertThat(page).containsEntry("templateId", 9);
		Map<String, Object> teaser = (Map<String, Object>) ((Map<String, Object>) page.get("tags")).get("teaser");
		assertThat(teaser).containsEntry("constructKeyword", "text").containsEntry("properties",
				Map.of("text", "Short"));
		assertThat(teaser).doesNotContainKey("propertiesRaw");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testIncludes() {
		GetPageTool.Result result = GetPageTool.result(PageFixtures.page(),
				List.of("tags", "template", "folder", "constructs"), Map.of());

		Map<String, Object> json = assertValidOutput(tool, result);
		assertThat((Map<String, Object>) json.get("template")).containsEntry("type", "template")
				.containsEntry("id", 9).containsEntry("nodeId", 3);
		assertThat((Map<String, Object>) json.get("folder")).containsEntry("type", "folder").containsEntry("id", 57);
		Map<String, Object> teaser = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) json
				.get("page")).get("tags")).get("teaser");
		assertThat(teaser).containsKey("propertiesRaw");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testWithoutTags() {
		Map<String, Object> json = assertValidOutput(tool,
				GetPageTool.result(PageFixtures.page(), List.of(), Map.of()));

		assertThat((Map<String, Object>) json.get("page")).doesNotContainKey("tags");
	}
}
