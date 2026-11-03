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

import com.gentics.contentnode.mcp.model.ConstructInfo;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.Part;

/**
 * Unit tests for {@link ListConstructsTool}. The filters are covered by the live check.
 */
public class ListConstructsToolTest {
	private final ListConstructsTool tool = new ListConstructsTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "list_constructs", "List constructs");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of());
		assertAccepted(tool, Map.of("nodeId", 1, "pageId", 8, "categoryId", 7, "partTypeIds", List.of(1, 43), "q",
				"teaser", "changeable", true, "size", 10, "from", 10));
		assertRejected(tool, Map.of("partTypeIds", List.of(1, 1)));
		assertRejected(tool, Map.of("nodeId", 0));
		assertRejected(tool, Map.of("keyword", "teaser"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of());
	}

	@Test
	public void testResult() {
		Construct teaser = new Construct().setId(12).setKeyword("teaser").setName("Teaser")
				.setParts(List.of(new Part().setKeyword("handlebars").setTypeId(43), new Part().setKeyword("text")
						.setTypeId(1)));
		Construct text = new Construct().setId(2).setKeyword("text").setName("Text")
				.setParts(List.of(new Part().setKeyword("text").setTypeId(1)));

		ListResult<ConstructInfo.Summary> result = ListConstructsTool.result(List.of(teaser, text),
				ListArgs.of(Map.of("size", 1), ListConstructsTool.LIMITS));

		assertThat(result.total()).isEqualTo(2);
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).keyword()).isEqualTo("teaser");
		assertThat(result.items().get(0).partKeywords()).containsExactly("text");
		assertThat(result.items().get(0).hasHandlebarsPart()).isTrue();
		assertValidOutput(tool, result);
	}
}
