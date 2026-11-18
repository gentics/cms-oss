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

/**
 * Unit tests for {@link DeleteTaskTool}. Deleting, the lookup by external key and the permission checks are covered
 * by the tests of the task resource and the live check.
 */
public class DeleteTaskToolTest {
	private final DeleteTaskTool tool = new DeleteTaskTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "delete_task", "Delete task");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", "12"));
		assertAccepted(tool, Map.of("externalKey", "genaix:s:p:release", "source", "genaix"));
		assertRejected(tool, Map.of("externalKey", "k", "source", "linkChecker"));
		assertRejected(tool, Map.of("id", "12", "status", "done"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", "12"));
	}

	@Test
	public void testNeedsExactlyOneAddress() {
		assertThatThrownBy(() -> tool.invoke(Map.of(), Optional.empty())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("exactly one");
		assertThatThrownBy(() -> tool.invoke(Map.of("id", "12", "externalKey", "k"), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly one");
	}

	@Test
	public void testOutput() {
		assertThat(assertValidOutput(tool, new DeleteTaskTool.Result(false, null))).doesNotContainKey("id");
		assertValidOutput(tool, new DeleteTaskTool.Result(true, "12"));
	}
}
