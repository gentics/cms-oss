package com.gentics.contentnode.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.auth.McpRequestCredentials;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.tools.ListNodesTool.LanguageInfo;
import com.gentics.contentnode.mcp.tools.ListNodesTool.ListNodesResult;
import com.gentics.contentnode.mcp.tools.ListNodesTool.NodeListItem;
import com.gentics.contentnode.mcp.tools.ListNodesTool.Slice;

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
 * {@link ListNodesTool#invoke} runs), argument parsing, the paging slice computation, and that the
 * result shape validates against the declared output schema. Listing real nodes with a real,
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
		ListNodesResult result = new ListNodesResult(3, true, 1, true,
				List.of(new NodeListItem(new ObjectRef(Type.NODE, 1, "A547.1", null, "Node", null, null, null, null),
						"www.example.com", "/", List.of(new LanguageInfo(1, "de", "Deutsch")), 10, null, null)));

		Map<String, Object> structured = MAPPER.convertValue(result, new TypeReference<Map<String, Object>>() {
		});
		ValidationResponse response = new DefaultJsonSchemaValidator().validate(tool.tool().outputSchema(), structured);

		assertThat(response.valid()).as(response.errorMessage()).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResultOmitsNullFields() {
		ListNodesResult result = new ListNodesResult(0, true, null, false, List.of(new NodeListItem(
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

	@Test
	public void testIntArg() {
		assertThat(ListNodesTool.intArg(Map.of(), "size", 25, 1, 200)).isEqualTo(25);
		assertThat(ListNodesTool.intArg(Map.of("size", 10), "size", 25, 1, 200)).isEqualTo(10);
		assertThat(ListNodesTool.intArg(Map.of("size", 10L), "size", 25, 1, 200)).isEqualTo(10);
		assertThat(ListNodesTool.intArg(Map.of("size", 10.0), "size", 25, 1, 200)).isEqualTo(10);
		assertThat(ListNodesTool.intArg(Map.of("size", "10"), "size", 25, 1, 200)).isEqualTo(10);
		assertThat(ListNodesTool.intArg(Map.of("size", 0), "size", 25, 1, 200)).isEqualTo(1);
		assertThat(ListNodesTool.intArg(Map.of("size", 1000), "size", 25, 1, 200)).isEqualTo(200);
		assertThatThrownBy(() -> ListNodesTool.intArg(Map.of("size", "abc"), "size", 25, 1, 200))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("size");
	}

	@Test
	public void testSliceFirstPage() {
		Slice slice = Slice.of(0, 2, 5);

		assertThat(slice.fromIndex()).isEqualTo(0);
		assertThat(slice.toIndex()).isEqualTo(2);
		assertThat(slice.truncated()).isTrue();
		assertThat(slice.nextFrom()).isEqualTo(2);
	}

	@Test
	public void testSliceMiddlePageNotAlignedToSize() {
		Slice slice = Slice.of(3, 2, 10);

		assertThat(slice.fromIndex()).isEqualTo(3);
		assertThat(slice.toIndex()).isEqualTo(5);
		assertThat(slice.nextFrom()).isEqualTo(5);
	}

	@Test
	public void testSliceLastPartialPage() {
		Slice slice = Slice.of(4, 2, 5);

		assertThat(slice.fromIndex()).isEqualTo(4);
		assertThat(slice.toIndex()).isEqualTo(5);
		assertThat(slice.truncated()).isFalse();
		assertThat(slice.nextFrom()).isNull();
	}

	@Test
	public void testSliceExactlyAtEnd() {
		Slice slice = Slice.of(3, 2, 5);

		assertThat(slice.toIndex()).isEqualTo(5);
		assertThat(slice.truncated()).isFalse();
		assertThat(slice.nextFrom()).isNull();
	}

	@Test
	public void testSliceFromBeyondTotal() {
		Slice slice = Slice.of(10, 2, 5);

		assertThat(slice.fromIndex()).isEqualTo(5);
		assertThat(slice.toIndex()).isEqualTo(5);
		assertThat(slice.truncated()).isFalse();
		assertThat(slice.nextFrom()).isNull();
	}

	@Test
	public void testSliceEmptyList() {
		Slice slice = Slice.of(0, 25, 0);

		assertThat(slice.fromIndex()).isEqualTo(0);
		assertThat(slice.toIndex()).isEqualTo(0);
		assertThat(slice.truncated()).isFalse();
		assertThat(slice.nextFrom()).isNull();
	}

	@Test
	public void testSliceNoOverflow() {
		Slice slice = Slice.of(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, Integer.MAX_VALUE);

		assertThat(slice.toIndex()).isEqualTo(Integer.MAX_VALUE);
		assertThat(slice.truncated()).isFalse();
	}

	@Test
	public void testSlicesCoverEveryItemExactlyOnce() {
		int total = 7;
		int size = 3;
		int covered = 0;
		Integer from = 0;
		while (from != null) {
			Slice slice = Slice.of(from, size, total);
			assertThat(slice.fromIndex()).isEqualTo(covered);
			covered = slice.toIndex();
			from = slice.nextFrom();
		}
		assertThat(covered).isEqualTo(total);
	}
}
