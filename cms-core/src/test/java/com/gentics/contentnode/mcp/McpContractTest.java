package com.gentics.contentnode.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Conformance test of the registered tools ({@link ManualMcpTools}) against the GenAIx tool
 * contract, a vendored, unmodified copy of {@code tools.json} in the test resources. Compares the
 * tool name, the phase, the required input members and the input property names. Descriptions,
 * output schemas and the contract's group/access/permission metadata are not compared, and not
 * every contract tool has to be registered yet.
 */
public class McpContractTest {
	/**
	 * Registered tools that are not part of the contract
	 */
	private static final Set<String> EXEMPT = Set.of("page_load");

	private static String version;

	private static Map<String, JsonNode> contractTools;

	@BeforeClass
	public static void loadContract() throws IOException {
		try (InputStream in = McpContractTest.class.getResourceAsStream("tools.json")) {
			JsonNode contract = new ObjectMapper().readTree(in);
			version = contract.get("version").asText();
			contractTools = new HashMap<>();
			for (JsonNode group : contract.get("groups")) {
				for (JsonNode tool : group.get("tools")) {
					contractTools.put(tool.get("name").asText(), tool);
				}
			}
		}
	}

	@Test
	public void testRegisteredToolsConform() {
		List<String> mismatches = new ArrayList<>();
		for (McpToolProvider provider : ManualMcpTools.tools()) {
			if (!EXEMPT.contains(provider.tool().name())) {
				mismatches.addAll(mismatches(provider.tool()));
			}
		}

		assertThat(mismatches).as("registered tools vs. tools.json " + version).isEmpty();
	}

	@Test
	public void testToolNotInContract() {
		Tool tool = Tool.builder().name("page_list").inputSchema(JsonSchema.builder().type("object").build()).build();

		assertThat(mismatches(tool)).containsExactly("page_list: not in tools.json " + version);
	}

	@Test
	public void testRequiredDrift() {
		Tool tool = Tool.builder().name("update_page_properties")
				.inputSchema(JsonSchema.builder().type("object")
						.properties(new HashMap<>(contractProperties("update_page_properties")))
						.required(List.of("id")).build())
				.build();

		assertThat(mismatches(tool))
				.containsExactly("update_page_properties: required [id], tools.json " + version + " [pageId]");
	}

	@Test
	public void testPropertyDrift() {
		Tool tool = Tool.builder().name("list_nodes")
				.inputSchema(JsonSchema.builder().type("object").properties(Map.of("q", Map.of(), "page", Map.of()))
						.build())
				.build();

		assertThat(mismatches(tool)).containsExactly(
				"list_nodes: properties [page, q], tools.json " + version + " [from, q, size]");
	}

	/**
	 * Compare a tool with its contract entry
	 * @param tool tool definition
	 * @return mismatches, empty if the tool conforms
	 */
	@SuppressWarnings("unchecked")
	private static List<String> mismatches(Tool tool) {
		JsonNode contract = contractTools.get(tool.name());
		if (contract == null) {
			return List.of("%s: not in tools.json %s".formatted(tool.name(), version));
		}

		List<String> mismatches = new ArrayList<>();
		String phase = contract.path("phase").asText("one");
		if (!"one".equals(phase)) {
			mismatches.add("%s: phase %s in tools.json %s".formatted(tool.name(), phase, version));
		}

		Set<String> required = sorted((List<String>) tool.inputSchema().get("required"));
		Set<String> contractRequired = new TreeSet<>();
		contract.path("inputSchema").path("required").forEach(name -> contractRequired.add(name.asText()));
		if (!required.equals(contractRequired)) {
			mismatches.add("%s: required %s, tools.json %s %s".formatted(tool.name(), required, version,
					contractRequired));
		}

		Map<String, Object> properties = (Map<String, Object>) tool.inputSchema().get("properties");
		Set<String> names = sorted(properties != null ? properties.keySet() : Set.of());
		Set<String> contractNames = sorted(contractProperties(tool.name()).keySet());
		if (!names.equals(contractNames)) {
			mismatches.add(
					"%s: properties %s, tools.json %s %s".formatted(tool.name(), names, version, contractNames));
		}
		return mismatches;
	}

	/**
	 * Get the input properties of a contract tool
	 * @param name tool name
	 * @return property name to (empty) schema
	 */
	private static Map<String, Object> contractProperties(String name) {
		Map<String, Object> properties = new HashMap<>();
		contractTools.get(name).path("inputSchema").path("properties").fieldNames()
				.forEachRemaining(field -> properties.put(field, Map.of()));
		return properties;
	}

	private static Set<String> sorted(Collection<String> values) {
		return values != null ? new TreeSet<>(values) : new TreeSet<>();
	}
}
