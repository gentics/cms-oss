package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.model.Problem;
import com.gentics.contentnode.mcp.model.SearchHit;

/**
 * Unit tests for {@link EnterpriseSearch}. The search itself needs the enterprise module and Elasticsearch and is
 * covered by the live check.
 */
public class EnterpriseSearchTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	public void testUnavailableWithoutModule() {
		assertThatThrownBy(() -> EnterpriseSearch.available("com.gentics.contentnode.NoSuchSearch", () -> true))
				.isInstanceOf(SearchUnavailableException.class).hasMessageContaining("not installed");
		Problem problem = Problem.of("search_content", catchUnavailable(() -> EnterpriseSearch.available(
				"com.gentics.contentnode.NoSuchSearch", () -> true)));
		assertThat(problem.type()).endsWith("search-unavailable");
		assertThat(problem.status()).isEqualTo(503);
		assertThat(problem.retryable()).isFalse();
	}

	@Test
	public void testUnavailableWithFeatureOff() {
		assertThatThrownBy(() -> EnterpriseSearch.available(String.class.getName(), () -> false))
				.isInstanceOf(SearchUnavailableException.class).hasMessageContaining("elasticsearch");
		assertThat(EnterpriseSearch.available(String.class.getName(), () -> true)).isEqualTo(String.class);
	}

	@Test
	public void testPermissionFilterDetected() throws Exception {
		assertThat(EnterpriseSearch.hasPermissionFilter(MAPPER.readTree(
				"{\"query\":{\"bool\":{\"must\":[],\"filter\":[{\"terms\":{\"groupId\":[2]}}]}}}"))).isTrue();
		assertThat(EnterpriseSearch.hasPermissionFilter(MAPPER.readTree(
				"{\"query\":{\"bool\":{\"must\":[],\"filter\":[]}}}"))).isFalse();
		assertThat(EnterpriseSearch.hasPermissionFilter(MAPPER.readTree("{\"query\":{\"match_all\":{}}}")))
				.isFalse();
	}

	@Test
	public void testTypeFromSourceOrHit() throws Exception {
		assertThat(EnterpriseSearch.type(MAPPER.readTree("{\"_id\":\"1\",\"_source\":{\"_type\":\"page\"}}")))
				.isEqualTo("page");
		assertThat(EnterpriseSearch.type(MAPPER.readTree("{\"_id\":\"1\",\"_type\":\"image\",\"_source\":{}}")))
				.isEqualTo("image");
		assertThat(EnterpriseSearch.type(MAPPER.readTree("{\"_id\":\"1\"}"))).isNull();
	}

	@Test
	public void testHit() throws Exception {
		JsonNode hit = MAPPER.readTree("{\"_index\":\"page_de\",\"_id\":\"4711\",\"_score\":1.5,\"_source\":{"
				+ "\"_type\":\"page\",\"languageCode\":\"de\",\"online\":[1,3],\"edited\":1700000000,"
				+ "\"templateId\":9,\"folderId\":57},\"highlight\":{\"content\":[\"a <em>b</em>\",\"c\"],"
				+ "\"name\":[\"<em>b</em>\"]}}");

		SearchHit result = EnterpriseSearch.hit(hit, "page", 4711, "A547.1", 3, "Home");

		assertThat(result.ref().id()).isEqualTo(4711);
		assertThat(result.ref().nodeId()).isEqualTo(3);
		assertThat(result.ref().language()).isEqualTo("de");
		assertThat(result.score()).isEqualTo(1.5);
		assertThat(result.snippets()).containsExactly("a <em>b</em>", "c", "<em>b</em>");
		assertThat(result.online()).isTrue();
		assertThat(result.edited()).isEqualTo(1700000000);
		assertThat(result.templateId()).isEqualTo(9);
		assertThat(result.folderId()).isEqualTo(57);
	}

	@Test
	public void testOnlineShapes() throws Exception {
		assertThat(online("[]")).isFalse();
		assertThat(online("3")).isTrue();
		assertThat(online("null")).isNull();
	}

	@Test
	public void testTotal() throws Exception {
		assertThat(EnterpriseSearch.total(MAPPER.readTree("{\"hits\":{\"total\":{\"value\":42,\"relation\":\"eq\"}}}")))
				.isEqualTo(42);
		assertThat(EnterpriseSearch.total(MAPPER.readTree("{\"hits\":{\"total\":7}}"))).isEqualTo(7);
	}

	@Test
	public void testReason() {
		assertThat(EnterpriseSearch.reason("{\"error\":{\"root_cause\":[{\"reason\":\"no such field\"}],"
				+ "\"reason\":\"all shards failed\"}}")).isEqualTo("no such field");
		assertThat(EnterpriseSearch.reason("not json")).isEqualTo("not json");
		assertThat(EnterpriseSearch.reason("x".repeat(600))).hasSize(500);
	}

	private static Boolean online(String value) throws Exception {
		return EnterpriseSearch.hit(MAPPER.readTree("{\"_source\":{\"online\":" + value + "}}"), "page", 1, null,
				null, null).online();
	}

	private static Throwable catchUnavailable(Runnable runnable) {
		try {
			runnable.run();
			return null;
		} catch (RuntimeException e) {
			return e;
		}
	}
}
