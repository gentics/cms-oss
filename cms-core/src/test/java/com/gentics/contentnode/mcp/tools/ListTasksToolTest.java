package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.rest.model.task.UserTaskModel;

/**
 * Unit tests for {@link ListTasksTool}. Listing, the filters and the visibility are covered by the tests of the task
 * resource and the live check.
 */
public class ListTasksToolTest {
	private final ListTasksTool tool = new ListTasksTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "list_tasks", "List tasks");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of());
		assertAccepted(tool, Map.of("source", List.of("genaix"), "genaixSessionId", "s", "status",
				List.of("open", "done"), "page", 2, "pageSize", 10));
		assertAccepted(tool, Map.of("source", List.of("publishQueue", "linkChecker")));
		assertRejected(tool, Map.of("status", List.of("finished")));
		assertRejected(tool, Map.of("status", "open"));
		assertRejected(tool, Map.of("pageSize", 101));
		assertRejected(tool, Map.of("page", 0));
		assertRejected(tool, Map.of("q", "terms"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of());
	}

	@Test
	public void testInvalidArgumentsRejectedBeforeAnyRead() {
		// the arguments are checked before the task resource is touched, which would fail here
		assertThatThrownBy(() -> tool.invoke(Map.of("status", List.of("open", "open")), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("more than once");
		assertThatThrownBy(() -> tool.invoke(Map.of("pageSize", 500), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testOutput() {
		assertValidOutput(tool, new ListTasksTool.Result(List.of(new UserTaskModel().setId("12").setTitle("Terms")),
				1, false));
	}
}
