package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests for {@link SearchBodies}
 */
public class SearchBodiesTest {
	@Test
	public void testBoolWithQuery() {
		ObjectNode body = SearchBodies.bool(List.of(SearchBodies.queryString("Nutzungsbedingungen")),
				SearchBodies.filters(null, null, null));

		JsonNode bool = body.path("query").path("bool");
		assertThat(bool.path("must").get(0).path("query_string").path("query").asText())
				.isEqualTo("Nutzungsbedingungen");
		assertThat(bool.path("must").get(0).path("query_string").path("fields").toString())
				.isEqualTo("[\"name^2\",\"description\",\"content\"]");
		assertThat(bool.path("filter").isArray()).isTrue();
		assertThat(bool.path("filter")).isEmpty();
	}

	@Test
	public void testBoolWithoutQueryMatchesAll() {
		JsonNode must = SearchBodies.bool(List.of(), List.of()).path("query").path("bool").path("must");

		assertThat(must.get(0).has("match_all")).isTrue();
	}

	@Test
	public void testFilters() {
		List<JsonNode> online = SearchBodies.filters(true, List.of(1, 2), 1700000000);
		List<JsonNode> offline = SearchBodies.filters(false, null, null);

		assertThat(online).extracting(JsonNode::toString).containsExactly("{\"exists\":{\"field\":\"online\"}}",
				"{\"terms\":{\"templateId\":[1,2]}}", "{\"range\":{\"edited\":{\"gte\":1700000000,\"format\":\"epoch_second\"}}}");
		assertThat(offline).extracting(JsonNode::toString)
				.containsExactly("{\"bool\":{\"must_not\":{\"exists\":{\"field\":\"online\"}}}}");
	}

	@Test
	public void testRawClauseIsWrapped() {
		ObjectNode body = SearchBodies.raw(Map.of("match", Map.of("name", "Terms")));

		assertThat(body.path("query").path("bool").path("must").get(0).toString())
				.isEqualTo("{\"match\":{\"name\":\"Terms\"}}");
		assertThat(body.path("query").path("bool").path("filter").isArray()).isTrue();
	}

	@Test
	public void testRawBodyIsWrapped() {
		ObjectNode body = SearchBodies.raw(Map.of("query", Map.of("term", Map.of("templateId", 9))));

		assertThat(body.path("query").path("bool").path("must").get(0).toString())
				.isEqualTo("{\"term\":{\"templateId\":9}}");
	}

	@Test
	public void testRawBoolIsKeptWithFilterArray() {
		ObjectNode body = SearchBodies.raw(Map.of("query", Map.of("bool", Map.of("should",
				List.of(Map.of("match", Map.of("name", "a"))), "filter", Map.of("term", Map.of("languageCode",
						"de"))))));

		JsonNode bool = body.path("query").path("bool");
		assertThat(bool.path("should").size()).isEqualTo(1);
		assertThat(bool.path("filter").isArray()).isTrue();
		assertThat(bool.path("filter").get(0).toString()).isEqualTo("{\"term\":{\"languageCode\":\"de\"}}");
	}

	@Test
	public void testUnwrappableRawQueriesRejected() {
		for (Map<String, Object> raw : List.<Map<String, Object>>of(
				Map.of("query", Map.of("bool", Map.of("filter", "x"))),
				Map.of("query", Map.of("bool", "x")),
				Map.of("query", Map.of("bool", Map.of(), "match", Map.of())),
				Map.of("query", Map.of("match_all", Map.of()), "aggs", Map.of()),
				Map.of("query", "x"),
				Map.of())) {
			assertThatThrownBy(() -> SearchBodies.raw(raw)).as("raw query %s", raw)
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	public void testHighlightAndMoreLikeThis() {
		ObjectNode body = SearchBodies.highlight(SearchBodies.bool(List.of(SearchBodies.moreLikeThis(
				SearchBodies.MAPPER.getNodeFactory().textNode("some text"))), List.of()));

		assertThat(body.path("highlight").path("fields").has("content")).isTrue();
		assertThat(body.path("highlight").path("fragment_size").asInt()).isEqualTo(200);
		JsonNode mlt = body.path("query").path("bool").path("must").get(0).path("more_like_this");
		assertThat(mlt.path("like").asText()).isEqualTo("some text");
		assertThat(mlt.path("fields").toString()).isEqualTo("[\"name\",\"description\",\"content\"]");
	}
}
