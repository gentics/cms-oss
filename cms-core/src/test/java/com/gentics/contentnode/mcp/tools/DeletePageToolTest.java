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

/**
 * Unit tests for {@link DeletePageTool}. Deleting a real page, with and without wastebin, is covered by the live check.
 */
public class DeletePageToolTest {
	private final DeletePageTool tool = new DeletePageTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "delete_page", "Delete page", "pageId");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("pageId", 4711));
		assertRejected(tool, Map.of());
		assertRejected(tool, Map.of("pageId", 4711, "purge", true));
	}

	@Test
	public void testRejectsChannel() {
		assertThatThrownBy(() -> tool.invoke(Map.of("pageId", 4711, "nodeId", 2), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("support nodeId");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("pageId", 4711));
	}

	@Test
	public void testResult() {
		Map<String, Object> json = assertValidOutput(tool,
				new DeletePageTool.Result(ObjectRef.of(ObjectRef.Type.PAGE, 4711), true, true));
		assertThat(json).containsEntry("deleted", true).containsEntry("restorable", true);
	}
}
