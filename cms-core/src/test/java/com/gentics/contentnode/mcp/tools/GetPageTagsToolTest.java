package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.TagInfo;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;

/**
 * Unit tests for {@link GetPageTagsTool}. Loading a real page is covered by the live check.
 */
public class GetPageTagsToolTest {
	private final GetPageTagsTool tool = new GetPageTagsTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_page_tags", "Get page tags", "id");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", 4711, "q", "teaser", "size", 200, "from", 10000));
		assertRejected(tool, Map.of("id", 4711, "size", 201));
		assertRejected(tool, Map.of("q", "teaser"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", 4711));
	}

	@Test
	public void testSecondPage() {
		List<Tag> tags = new ArrayList<>();
		for (int i = 30; i > 0; i--) {
			tags.add(PageFixtures.tag("content%02d".formatted(i), 5, Property.Type.RICHTEXT, "x"));
		}

		GetPageTagsTool.Result result = GetPageTagsTool.result(PageFixtures.page(), null, tags, Map.of(),
				ListArgs.of(Map.of("size", 25, "from", 25), GetPageTagsTool.LIMITS));

		assertThat(result.list().total()).isEqualTo(30);
		assertThat(result.list().truncated()).isFalse();
		assertThat(result.list().items()).extracting(TagInfo::name).containsExactly("content26", "content27",
				"content28", "content29", "content30");
		assertValidOutput(tool, result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testQueryMatchesNameOrKeyword() {
		Page page = PageFixtures.page();
		List<Tag> tags = new ArrayList<>(page.getTags().values());
		Map<Integer, String> keywords = Map.of(8, "text", 5, "richtext");

		GetPageTagsTool.Result byKeyword = GetPageTagsTool.result(page, 3, tags, keywords,
				ListArgs.of(Map.of("q", "RICH"), GetPageTagsTool.LIMITS));
		GetPageTagsTool.Result byName = GetPageTagsTool.result(page, 3, tags, keywords,
				ListArgs.of(Map.of("q", "tease"), GetPageTagsTool.LIMITS));

		assertThat(byKeyword.list().items()).extracting(TagInfo::name).containsExactly("content1");
		assertThat(byName.list().items()).extracting(TagInfo::name).containsExactly("teaser");
		Map<String, Object> json = assertValidOutput(tool, byName);
		assertThat(json).containsKeys("pageRef", "items", "total");
		assertThat((Map<String, Object>) json.get("pageRef")).containsEntry("nodeId", 3);
	}
}
