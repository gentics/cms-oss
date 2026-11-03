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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.model.SearchHit;
import com.gentics.contentnode.mcp.util.EnterpriseSearch;

/**
 * Unit tests for {@link FindSimilarTool}. Searching needs the enterprise module and Elasticsearch and is covered by
 * the live check.
 */
public class FindSimilarToolTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final FindSimilarTool tool = new FindSimilarTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "find_similar", "Find similar objects");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("ref", Map.of("type", "page", "id", 4711), "size", 5, "minScore", 0.5));
		assertAccepted(tool, Map.of("text", "Terms of use", "types", List.of("page", "form")));
		assertRejected(tool, Map.of("text", "x", "minScore", -1));
		assertRejected(tool, Map.of("text", "x", "folderId", List.of(1)));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("text", "x"));
	}

	@Test
	public void testExactlyOneOfRefAndText() {
		assertThatThrownBy(() -> FindSimilarTool.Request.of(Map.of())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("exactly one");
		assertThatThrownBy(() -> FindSimilarTool.Request.of(Map.of("text", "x", "ref", Map.of("type", "page", "id",
				1)))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly one");
		assertThatThrownBy(() -> FindSimilarTool.Request.of(Map.of("ref", Map.of("type", "template", "id", 1))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("template");
	}

	@Test
	public void testBody() {
		FindSimilarTool.Request byRef = FindSimilarTool.Request.of(Map.of("ref", Map.of("type", "page", "id", 4711),
				"size", 5, "minScore", 0.5));

		ObjectNode body = FindSimilarTool.body(MAPPER.createArrayNode().add(MAPPER.createObjectNode()
				.put("_index", "page_de").put("_id", "4711")), byRef);

		assertThat(body.path("query").path("bool").path("must").get(0).path("more_like_this").path("like")
				.toString()).isEqualTo("[{\"_index\":\"page_de\",\"_id\":\"4711\"}]");
		assertThat(body.path("size").asInt()).isEqualTo(6);
		assertThat(body.path("min_score").asDouble()).isEqualTo(0.5);
	}

	@Test
	public void testOwnObjectRemoved() {
		FindSimilarTool.Request byRef = FindSimilarTool.Request.of(Map.of("ref", Map.of("type", "page", "id", 4711),
				"size", 2));
		EnterpriseSearch.Response response = new EnterpriseSearch.Response(3, 1, List.of(hit(Type.PAGE, 4711),
				hit(Type.PAGE, 1), hit(Type.FILE, 4711)), null, MAPPER.createObjectNode());

		FindSimilarTool.Result result = FindSimilarTool.result(response, byRef);

		assertThat(result.hits()).extracting(hit -> hit.ref().type() + ":" + hit.ref().id())
				.containsExactly("PAGE:1", "FILE:4711");
		assertValidOutput(tool, result);
	}

	private static EnterpriseSearch.Hit hit(Type type, int id) {
		return new EnterpriseSearch.Hit(new SearchHit(ObjectRef.of(type, id), 1.0, List.of(), null, null, null,
				null, null), "index");
	}
}
