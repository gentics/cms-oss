package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.TagInfo;
import com.gentics.contentnode.mcp.tools.AddPageTagTool.Request;
import com.gentics.contentnode.rest.model.Tag;

/**
 * Unit tests for {@link AddPageTagTool}. Adding a tag to a real page is covered by the live check.
 */
public class AddPageTagToolTest {
	private final AddPageTagTool tool = new AddPageTagTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "add_page_tag", "Add page tag", "pageId");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("pageId", 4711, "constructKeyword", "hero"));
		assertAccepted(tool, Map.of("pageId", 4711, "copyFrom", Map.of("pageId", 8, "tagName", "content1")));
		assertRejected(tool, Map.of("constructKeyword", "hero"));
		assertRejected(tool, Map.of("pageId", 4711, "copyFrom", Map.of("pageId", 8)));
		assertRejected(tool, Map.of("pageId", 4711, "copyFrom", Map.of("pageId", 8, "tagName", "c", "x", 1)));
	}

	@Test
	public void testRejectsChannel() {
		assertThatThrownBy(() -> tool.invoke(Map.of("pageId", 4711, "constructId", 2, "nodeId", 2), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("support nodeId");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("pageId", 4711, "constructKeyword", "hero"));
	}

	@Test
	public void testRequest() {
		assertThat(Request.of(Map.of("pageId", 4711, "constructKeyword", "hero")))
				.isEqualTo(new Request(4711, "hero", null, null, null));
		assertThat(Request.of(Map.of("pageId", 4711, "constructId", 2)))
				.isEqualTo(new Request(4711, null, 2, null, null));
		assertThat(Request.of(Map.of("pageId", 4711, "copyFrom", Map.of("pageId", 8, "tagName", "content1"))))
				.isEqualTo(new Request(4711, null, null, 8, "content1"));
	}

	@Test
	public void testRequestRejections() {
		assertRequestRejected(Map.of("pageId", 4711), "exactly one");
		assertRequestRejected(Map.of("pageId", 4711, "constructKeyword", "hero", "constructId", 2), "exactly one");
		assertRequestRejected(Map.of("pageId", 4711, "constructKeyword", "hero", "tagName", "hero1"),
				"'tagName' is not supported");
		assertRequestRejected(Map.of("pageId", 4711, "constructId", 2, "copyFrom",
				Map.of("pageId", 8, "tagName", "c")), "not both");
		assertRequestRejected(Map.of("pageId", 4711, "copyFrom", Map.of("pageId", 8)), "copyFrom.tagName");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResult() {
		Tag tag = new Tag();
		tag.setName("hero3");
		tag.setConstructId(12);
		tag.setActive(true);
		tag.setType(Tag.Type.CONTENTTAG);

		Map<String, Object> json = assertValidOutput(tool, new AddPageTagTool.Result(
				TagInfo.of(tag, Map.of(12, "hero"), false), ObjectRef.of(ObjectRef.Type.PAGE, 4711)));
		assertThat((Map<String, Object>) json.get("tag")).containsEntry("name", "hero3")
				.containsEntry("constructKeyword", "hero");
	}

	private static void assertRequestRejected(Map<String, Object> arguments, String message) {
		assertThatThrownBy(() -> Request.of(arguments)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(message);
	}
}
