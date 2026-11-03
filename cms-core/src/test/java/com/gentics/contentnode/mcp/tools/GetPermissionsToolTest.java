package com.gentics.contentnode.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.model.ObjectRef;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.util.ToolInputValidator;

/**
 * Unit tests for {@link GetPermissionsTool}.
 *
 * <p>
 * Only the tool definition, the argument validation, the unauthenticated-call rejection and the
 * output schema are unit-testable without a DB/CMS instance. Checking real permissions is covered by
 * {@code GetPermissionsToolIntegrationTest}.
 * </p>
 */
public class GetPermissionsToolTest {
	private final GetPermissionsTool tool = new GetPermissionsTool();

	@Test
	@SuppressWarnings("unchecked")
	public void testToolDefinition() throws Exception {
		Tool definition = tool.tool();

		assertThat(definition.name()).isEqualTo("get_permissions");
		assertThat(definition.title()).isEqualTo("Get effective permissions");
		assertThat(definition.inputSchema()).containsEntry("required", List.of("type"))
				.containsEntry("additionalProperties", false);
		assertThat(definition.outputSchema()).containsEntry("required", List.of("granted"));

		Map<String, Object> properties = (Map<String, Object>) definition.inputSchema().get("properties");
		Map<String, Object> permissions = (Map<String, Object>) properties.get("permissions");
		assertThat(permissions).containsEntry("maxItems", 20).containsEntry("uniqueItems", true);

		JsonNode contract = contractInputSchema();
		assertThat(((Map<String, Object>) properties.get("type")).get("enum"))
				.isEqualTo(strings(contract.at("/properties/type/enum")));
		assertThat(((Map<String, Object>) permissions.get("items")).get("enum"))
				.isEqualTo(strings(contract.at("/properties/permissions/items/enum")));
		assertThat(definition.description()).isEqualTo(contractTool().get("description").asText());
	}

	@Test
	public void testSdkInputValidation() {
		Tool definition = tool.tool();
		DefaultJsonSchemaValidator validator = new DefaultJsonSchemaValidator();

		assertThat(ToolInputValidator.validate(definition,
				Map.of("type", "page", "id", 7, "nodeId", 2, "permissions", List.of("view", "edit")), true, validator))
				.isNull();

		List<Map<String, Object>> invalid = List.of(Map.of(), Map.of("type", "tag"), Map.of("type", "page", "id", 0),
				Map.of("type", "page", "permissions", List.of("view", "view")),
				Map.of("type", "page", "permissions", List.of("fly")),
				Map.of("type", "page", "permissions", Collections.nCopies(21, "view")),
				Map.of("type", "page", "owner", "me"));
		for (Map<String, Object> arguments : invalid) {
			CallToolResult result = ToolInputValidator.validate(definition, arguments, true, validator);
			assertThat(result).as("validation of %s", arguments).isNotNull();
			assertThat(result.isError()).isTrue();
		}
	}

	@Test
	public void testRequest() {
		assertThat(GetPermissionsTool.Request.of(Map.of("type", "page")))
				.isEqualTo(new GetPermissionsTool.Request("page", null, null, null));
		assertThat(GetPermissionsTool.Request
				.of(Map.of("type", "folder", "id", 57, "nodeId", 2, "permissions", List.of("publishpages"))))
				.isEqualTo(new GetPermissionsTool.Request("folder", 57, 2, List.of("publishpages")));
		assertThat(GetPermissionsTool.Request.of(Map.of("type", "group", "permissions", List.of("userassignment")))
				.permissions()).containsExactly("userassignment");
	}

	@Test
	public void testRequestRejections() {
		assertThatThrownBy(() -> GetPermissionsTool.Request.of(Map.of())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("'type'");
		assertThatThrownBy(() -> GetPermissionsTool.Request.of(Map.of("type", "tag")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'tag'");
		assertThatThrownBy(() -> GetPermissionsTool.Request.of(Map.of("type", "page", "id", 0)))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'id'");
		assertThatThrownBy(() -> GetPermissionsTool.Request.of(Map.of("type", "page", "permissions", "view")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'permissions'");
		assertThatThrownBy(() -> GetPermissionsTool.Request.of(Map.of("type", "page", "permissions", List.of("fly"))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'fly'");
		assertThatThrownBy(
				() -> GetPermissionsTool.Request.of(Map.of("type", "page", "permissions", List.of("view", "view"))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("more than once");
		assertThatThrownBy(() -> GetPermissionsTool.Request
				.of(Map.of("type", "page", "permissions", new ArrayList<>(Collections.nCopies(21, "view")))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at most 20");
	}

	/**
	 * userassignment is no per-object permission of the CMS, so asking for it on an object is
	 * rejected before any CMS access
	 */
	@Test
	public void testUserassignmentWithIdIsRejected() {
		assertThatThrownBy(() -> GetPermissionsTool.Request
				.of(Map.of("type", "page", "id", 4711, "permissions", List.of("userassignment"))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("userassignment");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		CallToolResult result = tool.call(McpTransportContext.EMPTY,
				CallToolRequest.builder("get_permissions").arguments(Map.of("type", "page", "id", 1)).build());

		assertThat(result.isError()).isTrue();
		assertThat(result.structuredContent()).isNull();
	}

	@Test
	public void testResultsMatchOutputSchema() {
		Map<String, Boolean> granted = new LinkedHashMap<>();
		granted.put("view", true);
		granted.put("edit", true);
		granted.put("publish", false);

		assertValid(new GetPermissionsTool.Result(ObjectRef.of(ObjectRef.Type.PAGE, 4711), "page", granted));
		assertValid(new GetPermissionsTool.Result(null, "construct", Map.of("updateconstructs", true)));
		assertValid(new GetPermissionsTool.Result(null, "admin", Map.of()));
	}

	@SuppressWarnings("unchecked")
	private void assertValid(GetPermissionsTool.Result result) {
		Map<String, Object> json = new ObjectMapper().convertValue(result, Map.class);
		ValidationResponse validation = new DefaultJsonSchemaValidator().validate(tool.tool().outputSchema(), json);
		assertThat(validation.valid()).as(validation.errorMessage()).isTrue();
	}

	private static JsonNode contractTool() throws Exception {
		try (InputStream in = GetPermissionsToolTest.class
				.getResourceAsStream("/com/gentics/contentnode/mcp/tools.json")) {
			for (JsonNode group : new ObjectMapper().readTree(in).get("groups")) {
				for (JsonNode tool : group.get("tools")) {
					if ("get_permissions".equals(tool.get("name").asText())) {
						return tool;
					}
				}
			}
		}
		throw new AssertionError("get_permissions not in tools.json");
	}

	private static JsonNode contractInputSchema() throws Exception {
		return contractTool().get("inputSchema");
	}

	private static List<String> strings(JsonNode array) {
		List<String> values = new ArrayList<>();
		array.forEach(value -> values.add(value.asText()));
		return values;
	}
}
