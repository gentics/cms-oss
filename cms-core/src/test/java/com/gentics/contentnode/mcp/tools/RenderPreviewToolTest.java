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

import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.PageVersion;
import com.gentics.contentnode.rest.model.response.Message;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

/**
 * Unit tests for {@link RenderPreviewTool}. Rendering a real page is covered by the live check.
 */
public class RenderPreviewToolTest {
	private final RenderPreviewTool tool = new RenderPreviewTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "render_preview", "Render page preview", "pageId");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("pageId", 4711, "mode", "view", "version", 2, "includeHtml", true, "maxChars",
				1000));
		assertRejected(tool, Map.of("pageId", 4711, "maxChars", 999));
		assertRejected(tool, Map.of("pageId", 4711, "mode", "live"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("pageId", 4711));
	}

	@Test
	public void testEditModeNeverReachesTheCms() {
		// the unauthenticated path answers first, so check the argument handling directly
		assertThat(tool.call(McpTransportContext.EMPTY, CallToolRequest.builder("render_preview")
				.arguments(Map.of("pageId", 4711, "mode", "edit")).build()).isError()).isTrue();
		assertThatThrownBy(() -> invokeWithoutCms(Map.of("pageId", 4711, "mode", "edit")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("edit mode would lock");
	}

	@Test
	public void testVersionTimestamp() throws Exception {
		Page page = page();

		assertThat(RenderPreviewTool.versionTimestamp(page, 2000)).isEqualTo(2000);
		assertThat(RenderPreviewTool.versionTimestamp(page, 2)).isEqualTo(2000);
		assertThat(RenderPreviewTool.versionTimestamp(page, 1)).isEqualTo(1000);
		assertThatThrownBy(() -> RenderPreviewTool.versionTimestamp(page, 3))
				.isInstanceOf(EntityNotFoundException.class);
	}

	@Test
	public void testHtmlCutAtMaxChars() {
		Message message = new Message();
		message.setMessage("Rendered with warnings");

		RenderPreviewTool.Result result = RenderPreviewTool.result(page(), 3, 2000, "x".repeat(1500), 1000,
				List.of(message), 1700000000);

		assertThat(result.html()).hasSize(1000);
		assertThat(result.truncated()).isTrue();
		assertThat(result.previewUrl()).isEqualTo("/rest/page/render/content/4711?nodeId=3&version=2000");
		assertThat(result.messages()).containsExactly("Rendered with warnings");
		assertValidOutput(tool, result);
	}

	@Test
	public void testWithoutHtml() {
		RenderPreviewTool.Result result = RenderPreviewTool.result(page(), null, null, null, 1000, null, 1700000000);

		assertThat(result.previewUrl()).isEqualTo("/rest/page/render/content/4711");
		assertThat(result.truncated()).isFalse();
		assertThat(assertValidOutput(tool, result)).doesNotContainKey("html");
	}

	private void invokeWithoutCms(Map<String, Object> arguments) throws Exception {
		new RenderPreviewTool() {
			Object run() throws Exception {
				return invoke(arguments, Optional.empty());
			}
		}.run();
	}

	private static Page page() {
		PageVersion v1 = new PageVersion();
		v1.setNumber("1.0");
		v1.setTimestamp(1000);
		PageVersion v2 = new PageVersion();
		v2.setNumber("2.0");
		v2.setTimestamp(2000);
		Page page = new Page();
		page.setId(4711);
		page.setVersions(List.of(v1, v2));
		return page;
	}
}
