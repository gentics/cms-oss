package com.gentics.contentnode.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.AbstractMcpTool;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.util.ToolInputValidator;

/**
 * Assertions shared by the unit tests of the read tools
 */
final class ToolAssertions {
	private static final DefaultJsonSchemaValidator VALIDATOR = new DefaultJsonSchemaValidator();

	private ToolAssertions() {
	}

	/**
	 * Assert the tool's name, title, required arguments and closed input schema
	 * @param tool tool
	 * @param name expected name
	 * @param title expected title
	 * @param required expected required arguments
	 */
	static void assertDefinition(AbstractMcpTool tool, String name, String title, String... required) {
		Tool definition = tool.tool();
		assertThat(definition.name()).isEqualTo(name);
		assertThat(definition.title()).isEqualTo(title);
		assertThat(definition.description()).isNotBlank();
		assertThat(definition.inputSchema()).containsEntry("required", List.of(required))
				.containsEntry("additionalProperties", false);
		assertThat(definition.outputSchema()).isNotNull();
		ValidationResponse response = VALIDATOR.validateSchema(definition.outputSchema());
		assertThat(response.valid()).as(response.errorMessage()).isTrue();
	}

	/**
	 * Assert that the SDK accepts the arguments
	 * @param tool tool
	 * @param arguments arguments
	 */
	static void assertAccepted(AbstractMcpTool tool, Map<String, Object> arguments) {
		assertThat(ToolInputValidator.validate(tool.tool(), arguments, true, VALIDATOR)).as("validation of %s", arguments)
				.isNull();
	}

	/**
	 * Assert that the SDK rejects the arguments
	 * @param tool tool
	 * @param arguments arguments
	 */
	static void assertRejected(AbstractMcpTool tool, Map<String, Object> arguments) {
		CallToolResult result = ToolInputValidator.validate(tool.tool(), arguments, true, VALIDATOR);
		assertThat(result).as("validation of %s", arguments).isNotNull();
		assertThat(result.isError()).isTrue();
	}

	/**
	 * Assert that a call without credentials is refused without a problem
	 * @param tool tool
	 * @param arguments valid arguments
	 */
	static void assertRejectsWithoutCredentials(AbstractMcpTool tool, Map<String, Object> arguments) {
		CallToolResult result = tool.call(McpTransportContext.EMPTY,
				CallToolRequest.builder(tool.tool().name()).arguments(arguments).build());
		assertThat(result.isError()).isTrue();
		assertThat(result.structuredContent()).isNull();
	}

	/**
	 * Assert that a result matches the tool's output schema
	 * @param tool tool
	 * @param result result
	 * @return result as JSON map
	 */
	@SuppressWarnings("unchecked")
	static Map<String, Object> assertValidOutput(AbstractMcpTool tool, Object result) {
		Map<String, Object> json = new ObjectMapper().convertValue(result, Map.class);
		ValidationResponse validation = VALIDATOR.validate(tool.tool().outputSchema(), json);
		assertThat(validation.valid()).as(validation.errorMessage()).isTrue();
		return json;
	}
}
