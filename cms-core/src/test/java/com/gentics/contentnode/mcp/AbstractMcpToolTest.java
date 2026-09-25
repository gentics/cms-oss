package com.gentics.contentnode.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.factory.Session;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Unit tests for the result serialization of {@link AbstractMcpTool}. Uses tools that do not
 * require authentication, so no session (and no DB) is involved.
 */
public class AbstractMcpToolTest {
	/**
	 * Result returned by the test tools
	 * @param value some value
	 */
	public record Result(String value) {
	}

	private static class TestTool extends AbstractMcpTool {
		private final Map<String, Object> outputSchema;

		TestTool(Map<String, Object> outputSchema) {
			this.outputSchema = outputSchema;
		}

		@Override
		public boolean requiresAuthentication() {
			return false;
		}

		@Override
		public Tool tool() {
			return Tool.builder().name("test").inputSchema(JsonSchema.builder().type("object").build())
					.outputSchema(outputSchema).build();
		}

		@Override
		protected Object invoke(Map<String, Object> arguments, Optional<Session> session) {
			return new Result("hello");
		}
	}

	private static CallToolResult call(AbstractMcpTool tool) {
		return tool.call(McpTransportContext.EMPTY, CallToolRequest.builder("test").arguments(Map.of()).build());
	}

	@Test
	public void testNoStructuredContentWithoutOutputSchema() {
		CallToolResult result = call(new TestTool(null));

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(((TextContent) result.content().get(0)).text()).isEqualTo("{\"value\":\"hello\"}");
		assertThat(result.structuredContent()).isNull();
	}

	@Test
	public void testStructuredContentWithOutputSchema() {
		Map<String, Object> outputSchema = Map.of("type", "object", "properties",
				Map.of("value", Map.of("type", "string")), "required", List.of("value"));

		CallToolResult result = call(new TestTool(outputSchema));

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(((TextContent) result.content().get(0)).text()).isEqualTo("{\"value\":\"hello\"}");
		assertThat(result.structuredContent()).isEqualTo(Map.of("value", "hello"));
	}
}
