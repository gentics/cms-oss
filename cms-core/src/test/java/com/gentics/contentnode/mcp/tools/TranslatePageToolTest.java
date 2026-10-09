package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.PageInfo;
import com.gentics.contentnode.rest.model.Page;

/**
 * Unit tests for {@link TranslatePageTool}. Translating a real page is covered by the live check.
 */
public class TranslatePageToolTest {
	private final TranslatePageTool tool = new TranslatePageTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "translate_page", "Translate page", "pageId", "language");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("pageId", 4711, "language", "en"));
		assertAccepted(tool, Map.of("pageId", 4711, "language", "en", "locked", false));
		assertRejected(tool, Map.of("pageId", 4711));
		assertRejected(tool, Map.of("pageId", 4711, "language", "e"));
	}

	@Test
	public void testRejectsChannel() {
		assertThatThrownBy(() -> tool.invoke(Map.of("pageId", 4711, "language", "en", "nodeId", 2), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("support nodeId");
		assertThatThrownBy(
				() -> tool.invoke(Map.of("pageId", 4711, "language", "en", "channelId", 2), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("support channelId");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("pageId", 4711, "language", "en"));
	}

	@Test
	public void testHasVariant() {
		Page en = page(4712, "en");
		Page source = page(4711, "de");
		source.setLanguageVariants(Map.of(2, en));

		assertThat(TranslatePageTool.hasVariant(source, "en")).isTrue();
		assertThat(TranslatePageTool.hasVariant(source, "de")).isTrue();
		assertThat(TranslatePageTool.hasVariant(source, "fr")).isFalse();
		assertThat(TranslatePageTool.hasVariant(page(4711, "de"), "en")).isFalse();
	}

	@Test
	public void testResult() {
		Map<String, Object> json = assertValidOutput(tool, new TranslatePageTool.Result(PageInfo.of(page(4712, "en")),
				ObjectRef.forPage(page(4711, "de")), true));
		assertThat(json).containsEntry("created", true).containsKey("sourceRef");
	}

	private static Page page(int id, String language) {
		Page page = new Page();
		page.setId(id);
		page.setName("Home");
		page.setLanguage(language);
		return page;
	}
}
