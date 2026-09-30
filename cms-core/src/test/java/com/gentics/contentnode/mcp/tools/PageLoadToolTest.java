package com.gentics.contentnode.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.auth.McpRequestCredentials;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Unit tests for {@link PageLoadTool}.
 *
 * <p>
 * Only the tool definition and the unauthenticated-call rejection (handled entirely by
 * {@code AbstractMcpTool#call}, before {@link PageLoadTool#invoke} ever runs) are unit-testable
 * without a DB/CMS instance. Loading a real page with a real, authenticated session is covered by
 * an integration test using {@code DBTestContext} instead - see {@code docs/mcp-tests.md}.
 * </p>
 */
public class PageLoadToolTest {
	private final PageLoadTool tool = new PageLoadTool();

	@Test
	public void testToolDefinition() {
		Tool definition = tool.tool();

		assertThat(definition.name()).isEqualTo("page_load");
		assertThat(definition.inputSchema()).containsEntry("required", List.of("id"));

		@SuppressWarnings("unchecked")
		Map<String, Object> properties = (Map<String, Object>) definition.inputSchema().get("properties");
		assertThat(properties).containsKeys("id", "nodeId");
	}

	@Test
	public void testRequiresAuthenticationByDefault() {
		assertThat(tool.requiresAuthentication()).isTrue();
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		McpTransportContext context = McpTransportContext.EMPTY;
		CallToolRequest request = CallToolRequest.builder("page_load").arguments(Map.of("id", "1")).build();

		CallToolResult result = tool.call(context, request);

		assertThat(result.isError()).isTrue();
		assertThat(result.content()).hasSize(1);
		assertThat(((TextContent) result.content().get(0)).text()).contains("authenticated CMS session");
	}

	@Test
	public void testRejectsCallWithEmptyCredentialsInContext() {
		McpTransportContext context = McpTransportContext
				.create(Map.of(McpRequestCredentials.CONTEXT_KEY, McpRequestCredentials.EMPTY));
		CallToolRequest request = CallToolRequest.builder("page_load").arguments(Map.of("id", "1")).build();

		CallToolResult result = tool.call(context, request);

		assertThat(result.isError()).isTrue();
	}
}
