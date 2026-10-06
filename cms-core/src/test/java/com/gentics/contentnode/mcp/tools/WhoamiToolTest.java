package com.gentics.contentnode.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.WhoamiUser;
import com.gentics.contentnode.rest.model.Group;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.util.ToolInputValidator;

/**
 * Unit tests for {@link WhoamiTool}.
 *
 * <p>
 * Only the tool definition, the input validation, the unauthenticated-call rejection and the output
 * mapping are unit-testable without a DB/CMS instance. Loading the real user of a token is covered
 * by {@code WhoamiToolIntegrationTest}.
 * </p>
 */
public class WhoamiToolTest {
	private final WhoamiTool tool = new WhoamiTool();

	@Test
	@SuppressWarnings("unchecked")
	public void testToolDefinition() {
		Tool definition = tool.tool();

		assertThat(definition.name()).isEqualTo("whoami");
		assertThat(definition.title()).isEqualTo("Who am I");
		assertThat(definition.description()).startsWith("Returns the CMS user this session acts as")
				.contains("use get_permissions for that");
		assertThat(definition.inputSchema()).containsEntry("required", List.of())
				.containsEntry("additionalProperties", false);
		Map<String, Object> properties = (Map<String, Object>) definition.inputSchema().get("properties");
		assertThat(properties).containsOnlyKeys("includeGroups");
		assertThat((Map<String, Object>) properties.get("includeGroups")).containsEntry("type", "boolean")
				.containsEntry("default", true);
		assertThat(definition.outputSchema()).containsEntry("required", List.of("user"));
	}

	@Test
	public void testSdkInputValidation() {
		Tool definition = tool.tool();
		DefaultJsonSchemaValidator validator = new DefaultJsonSchemaValidator();

		assertThat(ToolInputValidator.validate(definition, Map.of(), true, validator)).isNull();
		assertThat(ToolInputValidator.validate(definition, Map.of("includeGroups", false), true, validator)).isNull();

		for (Map<String, Object> arguments : List.<Map<String, Object>>of(Map.of("login", "admin"),
				Map.of("includeGroups", "yes"))) {
			CallToolResult result = ToolInputValidator.validate(definition, arguments, true, validator);
			assertThat(result).as("validation of %s", arguments).isNotNull();
			assertThat(result.isError()).isTrue();
		}
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		CallToolResult result = tool.call(McpTransportContext.EMPTY,
				CallToolRequest.builder("whoami").arguments(Map.of()).build());

		assertThat(result.isError()).isTrue();
		assertThat(result.structuredContent()).isNull();
	}

	@Test
	public void testFullResultMatchesOutputSchema() {
		WhoamiTool.Result result = new WhoamiTool.Result(
				new WhoamiUser(42, "editor", "Edith", "Editor", "e@example.com"),
				List.of(ObjectRef.forGroup(group(7, "Editors"))));

		assertValid(result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMinimalResultMatchesOutputSchema() {
		WhoamiTool.Result result = new WhoamiTool.Result(new WhoamiUser(42, "editor", null, null, null), null);

		Map<String, Object> json = assertValid(result);
		assertThat(json).containsOnlyKeys("user");
		assertThat((Map<String, Object>) json.get("user")).containsOnlyKeys("id", "login");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> assertValid(WhoamiTool.Result result) {
		Map<String, Object> json = new ObjectMapper().convertValue(result, Map.class);
		ValidationResponse validation = new DefaultJsonSchemaValidator().validate(tool.tool().outputSchema(), json);
		assertThat(validation.valid()).as(validation.errorMessage()).isTrue();
		return json;
	}

	private static Group group(int id, String name) {
		Group group = new Group();
		group.setId(id);
		group.setName(name);
		return group;
	}
}
