package com.gentics.contentnode.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.Problem;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Unit tests for the result serialization and the shared static helpers of
 * {@link AbstractMcpTool}. Uses tools that do not require authentication, so no session (and no
 * DB) is involved.
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

	private static class FailingTool extends AbstractMcpTool {
		private final Map<String, Object> outputSchema;

		private final Exception failure;

		FailingTool(Map<String, Object> outputSchema, Exception failure) {
			this.outputSchema = outputSchema;
			this.failure = failure;
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
		protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
			throw failure;
		}
	}

	private static class AuthenticatedTool extends TestTool {
		AuthenticatedTool() {
			super(null);
		}

		@Override
		public boolean requiresAuthentication() {
			return true;
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

	@Test
	public void testErrorResultCarriesProblem() {
		Map<String, Object> outputSchema = Map.of("type", "object", "properties",
				Map.of("value", Map.of("type", "string")), "required", List.of("value"));

		CallToolResult result = call(new FailingTool(outputSchema, new EntityNotFoundException("Page 7 not found")));

		assertThat(result.isError()).isTrue();
		assertThat(((TextContent) result.content().get(0)).text()).isEqualTo("Page 7 not found");
		@SuppressWarnings("unchecked")
		Map<String, Object> problem = (Map<String, Object>) ((Map<String, Object>) result.structuredContent())
				.get("problem");
		assertThat(problem).containsEntry("type", Problem.TYPE_PREFIX + "not-found").containsEntry("status", 404)
				.containsEntry("tool", "test").containsEntry("retryable", false)
				.containsEntry("detail", "Page 7 not found");
	}

	@Test
	public void testUnauthenticatedCallHasNoProblem() {
		CallToolResult result = call(new AuthenticatedTool());

		assertThat(result.isError()).isTrue();
		assertThat(((TextContent) result.content().get(0)).text()).contains("authenticated CMS session");
		assertThat(result.structuredContent()).isNull();
	}

	@Test
	public void testRequireOkCause() {
		GenericResponse response = new GenericResponse(new Message(Message.Type.CRITICAL, "Name already used."),
				new ResponseInfo(ResponseCode.INVALIDDATA, "Error"));

		assertThatThrownBy(() -> AbstractMcpTool.requireOk(response, "Page 7 was not saved"))
				.isInstanceOf(IllegalArgumentException.class).cause().isInstanceOf(RestMappedException.class)
				.satisfies(cause -> assertThat(((RestMappedException) cause).getRestResponse()).isSameAs(response));
	}

	@Test
	public void testErrorMessage() {
		GenericResponse withMessage = new GenericResponse(new Message(Message.Type.CRITICAL, "Name already used."),
				new ResponseInfo(ResponseCode.INVALIDDATA, "Error while saving page 7: Name already used.", "name"));
		assertThat(AbstractMcpTool.errorMessage("Page 7 was not saved", withMessage))
				.isEqualTo("Page 7 was not saved: Name already used.");

		GenericResponse withoutMessage = new GenericResponse(null,
				new ResponseInfo(ResponseCode.INVALIDDATA, "Error while saving page."));
		assertThat(AbstractMcpTool.errorMessage("Page 7 was not saved", withoutMessage))
				.isEqualTo("Page 7 was not saved: Error while saving page.");

		assertThat(AbstractMcpTool.errorMessage("Page 7 was not saved", new GenericResponse()))
				.isEqualTo("Page 7 was not saved: unknown error");
	}

	@Test
	public void testRequireOk() {
		AbstractMcpTool.requireOk(new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Saved.")), "Not saved");

		assertThatThrownBy(() -> AbstractMcpTool.requireOk(new GenericResponse(
				new Message(Message.Type.CRITICAL, "Name already used."),
				new ResponseInfo(ResponseCode.INVALIDDATA, "Error")), "Page 7 was not saved"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Page 7 was not saved: Name already used.");
		assertThatThrownBy(() -> AbstractMcpTool.requireOk(new GenericResponse(), "Page 7 was not saved"))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Page 7 was not saved: unknown error");
		assertThatThrownBy(() -> AbstractMcpTool.requireOk(null, "Page 7 was not saved"))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Page 7 was not saved: unknown error");
	}

	/**
	 * Only the successful save: releasing the lock after a failed save needs a DB, and is covered by
	 * {@code UpdatePagePropertiesToolIntegrationTest} (rejected saves leave the page unlocked).
	 */
	@Test
	public void testSaveOrReleaseLockReturnsOkResponse() throws Exception {
		GenericResponse ok = new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Saved."));

		assertThat(AbstractMcpTool.saveOrReleaseLock(com.gentics.contentnode.object.Page.class, "7", "Not saved",
				() -> ok)).isSameAs(ok);
	}

	@Test
	public void testArgDelegates() {
		Map<String, Object> arguments = Map.of("name", "abc", "size", 10, "flag", true);

		assertThat(AbstractMcpTool.stringArg(arguments, "name", 1, 5)).isEqualTo("abc");
		assertThat(AbstractMcpTool.intArg(arguments, "size", 1, 200)).isEqualTo(10);
		assertThat(AbstractMcpTool.intArg(arguments, "from", 0, 100, 7)).isEqualTo(7);
		assertThat(AbstractMcpTool.booleanArg(arguments, "flag", false)).isTrue();
		assertThat(AbstractMcpTool.nullIfBlank(" ")).isNull();
		assertThatThrownBy(() -> AbstractMcpTool.intArg(Map.of("size", 0), "size", 1, 200))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'size'");
	}

	@Test
	public void testSchemaDelegates() {
		assertThat(AbstractMcpTool.schema("integer", "Some number.", "minimum", 1))
				.isEqualTo(McpSchemas.schema("integer", "Some number.", "minimum", 1));
		assertThat(AbstractMcpTool.objectRefSchema("Reference.")).isEqualTo(ObjectRef.jsonSchema("Reference."));
	}
}
