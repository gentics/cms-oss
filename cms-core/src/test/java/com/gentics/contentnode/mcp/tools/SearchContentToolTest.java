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

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.model.SearchHit;
import com.gentics.contentnode.mcp.util.SearchArgs;

/**
 * Unit tests for {@link SearchContentTool}. Searching needs the enterprise module and Elasticsearch and is covered by
 * the live check; the search-unavailable answer by {@code EnterpriseSearchTest}.
 */
public class SearchContentToolTest {
	private final SearchContentTool tool = new SearchContentTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "search_content", "Search content", "query");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("query", "Nutzungsbedingungen", "types", List.of("page", "file"), "nodeId", 3,
				"folderId", List.of(57), "recursive", false, "languages", List.of("de"), "filters",
				Map.of("online", true, "templateIds", List.of(9), "editedAfter", 1700000000), "size", 50, "from", 10,
				"rawQuery", Map.of("match", Map.of("name", "Terms"))));
		assertRejected(tool, Map.of());
		assertRejected(tool, Map.of("query", ""));
		assertRejected(tool, Map.of("query", "x", "size", 51));
		assertRejected(tool, Map.of("query", "x", "types", List.of("template")));
		assertRejected(tool, Map.of("query", "x", "filters", Map.of("language", "de")));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("query", "x"));
	}

	@Test
	public void testBodyFromArguments() {
		SearchArgs args = SearchArgs.of(Map.of("filters", Map.of("online", false, "templateIds", List.of(9))));

		ObjectNode body = SearchContentTool.body("Nutzungsbedingungen", args, null, 2, 4);

		JsonNode bool = body.path("query").path("bool");
		assertThat(bool.path("must").get(0).path("query_string").path("query").asText())
				.isEqualTo("Nutzungsbedingungen");
		assertThat(bool.path("filter").toString()).isEqualTo(
				"[{\"bool\":{\"must_not\":{\"exists\":{\"field\":\"online\"}}}},{\"terms\":{\"templateId\":[9]}}]");
		assertThat(body.path("size").asInt()).isEqualTo(2);
		assertThat(body.path("from").asInt()).isEqualTo(4);
		assertThat(body.path("track_total_hits").asBoolean()).isTrue();
		assertThat(body.has("highlight")).isTrue();
		assertThat(args.scope().types()).containsExactly("page");
		assertThat(args.scope().recursive()).isTrue();
	}

	@Test
	public void testRawQueryReplacesQueryAndFilters() {
		SearchArgs args = SearchArgs.of(Map.of("filters", Map.of("online", true)));

		ObjectNode body = SearchContentTool.body("x", args, Map.of("match", Map.of("name", "Terms")), 10, 0);

		assertThat(body.path("query").path("bool").path("must").toString())
				.isEqualTo("[{\"match\":{\"name\":\"Terms\"}}]");
		assertThat(body.path("query").path("bool").path("filter")).isEmpty();
	}

	@Test
	public void testUnwrappableRawQuery() {
		assertThatThrownBy(() -> SearchContentTool.body("x", SearchArgs.of(Map.of()),
				Map.of("query", Map.of("bool", Map.of("filter", "x"))), 10, 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testResultMatchesOutputSchema() {
		SearchHit hit = new SearchHit(new ObjectRef(Type.PAGE, 4711, null, 3, "Home", null, "de", null, null), 1.5,
				List.of("a <em>b</em>"), "de", true, 1700000000, 9, 57);
		ObjectNode queryUsed = SearchContentTool.body("x", SearchArgs.of(Map.of()), null, 10, 0);

		Map<String, Object> json = assertValidOutput(tool, new SearchContentTool.Result(12, false, 5, List.of(hit),
				queryUsed));

		assertThat(json).containsEntry("total", 12L).containsEntry("totalIsExact", false).containsKey("queryUsed");
	}
}
