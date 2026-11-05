package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.contentnode.mcp.util.SearchArgs;

/**
 * Unit tests for {@link CountContentTool}. Counting needs the enterprise module and Elasticsearch and is covered by
 * the live check.
 */
public class CountContentToolTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final CountContentTool tool = new CountContentTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "count_content", "Count and group content");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of());
		assertAccepted(tool, Map.of("query", "x", "types", List.of("page"), "groupBy", "template", "groupLimit",
				100));
		assertRejected(tool, Map.of("groupBy", "folder"));
		assertRejected(tool, Map.of("groupLimit", 101));
		assertRejected(tool, Map.of("size", 10));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of());
	}

	@Test
	public void testBodyWithoutGroups() {
		ObjectNode body = CountContentTool.body(null, SearchArgs.of(Map.of()), null, 25);

		assertThat(body.path("query").path("bool").path("must").get(0).has("match_all")).isTrue();
		assertThat(body.path("size").asInt()).isZero();
		assertThat(body.has("aggs")).isFalse();
	}

	@Test
	public void testAggregations() {
		assertThat(CountContentTool.body("x", SearchArgs.of(Map.of()), "language", 5).path("aggs").toString())
				.isEqualTo("{\"groups\":{\"terms\":{\"field\":\"languageCode\",\"size\":5}}}");
		assertThat(CountContentTool.body("x", SearchArgs.of(Map.of()), "template", 25).path("aggs").toString())
				.isEqualTo("{\"groups\":{\"terms\":{\"field\":\"templateId\",\"size\":25}}}");
		assertThat(CountContentTool.body("x", SearchArgs.of(Map.of()), "online", 25).path("aggs").toString())
				.isEqualTo("{\"groups\":{\"filters\":{\"filters\":{\"true\":{\"exists\":{\"field\":\"online\"}},"
						+ "\"false\":{\"bool\":{\"must_not\":{\"exists\":{\"field\":\"online\"}}}}}}}}");
	}

	@Test
	public void testGroups() throws Exception {
		List<CountContentTool.Group> terms = CountContentTool.groups(MAPPER.readTree(
				"{\"groups\":{\"buckets\":[{\"key\":\"de\",\"doc_count\":20},{\"key\":9,\"doc_count\":3}]}}"));
		List<CountContentTool.Group> filters = CountContentTool.groups(MAPPER.readTree(
				"{\"groups\":{\"buckets\":{\"true\":{\"doc_count\":30},\"false\":{\"doc_count\":5}}}}"));

		assertThat(terms).extracting(CountContentTool.Group::key).containsExactly("de", "9");
		assertThat(terms).extracting(CountContentTool.Group::count).containsExactly(20L, 3L);
		assertThat(filters).extracting(CountContentTool.Group::key).containsExactly("true", "false");
		assertThat(CountContentTool.groups(null)).isEmpty();
		assertValidOutput(tool, new CountContentTool.Result(35, false, 3, "online", filters,
				CountContentTool.body(null, SearchArgs.of(Map.of()), "online", 25)));
	}
}
