package com.gentics.contentnode.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.auth.McpRequestCredentials;
import com.gentics.contentnode.mcp.model.LanguageInfo;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.model.NodeInfo;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Unit tests for {@link ListNodesTool}.
 *
 * <p>
 * Covers everything that does not need a DB/CMS instance: the tool definition, the
 * unauthenticated-call rejection (handled by {@code AbstractMcpTool#call} before
 * {@link ListNodesTool#invoke} runs), and that the result shape validates against the declared
 * output schema. Argument parsing and the paging slice computation are covered by
 * {@code ListArgsTest} and {@code SliceTest}. Listing real nodes with a real,
 * authenticated session is covered by
 * {@code com.gentics.contentnode.tests.mcp.ListNodesToolIntegrationTest} and the manual protocol
 * in {@code docs/mcp-tests.md}.
 * </p>
 */
public class ListNodesToolTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final ListNodesTool tool = new ListNodesTool();

	@Test
	@SuppressWarnings("unchecked")
	public void testToolDefinition() {
		Tool definition = tool.tool();

		assertThat(definition.name()).isEqualTo("list_nodes");
		assertThat(definition.description()).contains("get_folder_tree");
		assertThat(definition.inputSchema()).containsEntry("type", "object").containsEntry("required", List.of())
				.containsEntry("additionalProperties", false);

		Map<String, Object> properties = (Map<String, Object>) definition.inputSchema().get("properties");
		assertThat(properties).containsOnlyKeys("q", "size", "from");

		assertThat((Map<String, Object>) properties.get("q")).containsEntry("type", "string")
				.containsEntry("maxLength", 200);
		assertThat((Map<String, Object>) properties.get("size")).containsEntry("type", "integer")
				.containsEntry("minimum", 1).containsEntry("maximum", 200).containsEntry("default", 25);
		assertThat((Map<String, Object>) properties.get("from")).containsEntry("type", "integer")
				.containsEntry("minimum", 0).containsEntry("maximum", 10000).containsEntry("default", 0);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOutputSchema() {
		Map<String, Object> outputSchema = tool.tool().outputSchema();

		assertThat(outputSchema).containsEntry("type", "object").containsEntry("required", List.of("items"));
		Map<String, Object> properties = (Map<String, Object>) outputSchema.get("properties");
		assertThat(properties).containsOnlyKeys("total", "totalIsExact", "nextFrom", "truncated", "items");

		Map<String, Object> itemProperties = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) properties
				.get("items")).get("items")).get("properties");
		assertThat(itemProperties).containsOnlyKeys("ref", "host", "publishDir", "languages", "defaultFileFolderId",
				"defaultImageFolderId", "contentRepositoryId");
	}

	@Test
	public void testOutputSchemaIsValidSchema() {
		ValidationResponse response = new DefaultJsonSchemaValidator().validateSchema(tool.tool().outputSchema());

		assertThat(response.valid()).as(response.errorMessage()).isTrue();
	}

	@Test
	public void testResultValidatesAgainstOutputSchema() {
		ListResult<NodeInfo> result = new ListResult<>(3, true, 1, true,
				List.of(new NodeInfo(new ObjectRef(Type.NODE, 1, "A547.1", null, "Node", null, null, null, null),
						"www.example.com", "/", List.of(new LanguageInfo(1, "de", "Deutsch")), 10, null, null)));

		Map<String, Object> structured = MAPPER.convertValue(result, new TypeReference<Map<String, Object>>() {
		});
		ValidationResponse response = new DefaultJsonSchemaValidator().validate(tool.tool().outputSchema(), structured);

		assertThat(response.valid()).as(response.errorMessage()).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResultOmitsNullFields() {
		ListResult<NodeInfo> result = new ListResult<>(0, true, null, false, List.of(new NodeInfo(
				ObjectRef.of(Type.NODE, 1), null, "/", List.of(), null, null, null)));

		Map<String, Object> structured = MAPPER.convertValue(result, new TypeReference<Map<String, Object>>() {
		});

		assertThat(structured).containsOnlyKeys("total", "totalIsExact", "truncated", "items");
		Map<String, Object> item = ((List<Map<String, Object>>) structured.get("items")).get(0);
		assertThat(item).containsOnlyKeys("ref", "publishDir", "languages");
	}

	@Test
	public void testRequiresAuthenticationByDefault() {
		assertThat(tool.requiresAuthentication()).isTrue();
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		CallToolRequest request = CallToolRequest.builder("list_nodes").arguments(Map.of()).build();

		CallToolResult result = tool.call(McpTransportContext.EMPTY, request);

		assertThat(result.isError()).isTrue();
		assertThat(result.content()).hasSize(1);
		assertThat(((TextContent) result.content().get(0)).text()).contains("authenticated CMS session");
	}

	@Test
	public void testRejectsCallWithEmptyCredentialsInContext() {
		McpTransportContext context = McpTransportContext
				.create(Map.of(McpRequestCredentials.CONTEXT_KEY, McpRequestCredentials.EMPTY));
		CallToolRequest request = CallToolRequest.builder("list_nodes").arguments(Map.of("q", "x")).build();

		CallToolResult result = tool.call(context, request);

		assertThat(result.isError()).isTrue();
	}
}
