package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.util.Tasks;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.model.task.UserTaskModel;

/**
 * Unit tests for {@link UpdateTaskTool}. Updating, the lookup by external key and the permission checks are covered
 * by the tests of the task resource and the live check.
 */
public class UpdateTaskToolTest {
	private final UpdateTaskTool tool = new UpdateTaskTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "update_task", "Update task");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", "12", "status", "done"));
		assertAccepted(tool, Map.of("externalKey", "genaix:s:p:release", "source", "genaix", "status", "done"));
		assertRejected(tool, Map.of("id", "12", "status", "finished"));
		assertRejected(tool, Map.of("id", "12", "type", "other"));
		assertRejected(tool, Map.of("id", 12, "status", "done"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", "12", "status", "done"));
	}

	@Test
	public void testAddressIsNoMemberOfTheChanges() {
		UserTaskModel changes = Tasks.model(Map.of("externalKey", "k", "source", "genaix", "status", "done"),
				Tasks.ARG_ID, Tasks.ARG_EXTERNAL_KEY, Tasks.ARG_SOURCE);
		assertThat(changes.getStatus()).isEqualTo(TaskStatus.done);
		assertThat(changes.getExternalKey()).isNull();
		assertThat(changes.getSource()).isNull();
	}

	@Test
	public void testNeedsExactlyOneAddress() {
		assertThatThrownBy(() -> tool.invoke(Map.of("status", "done"), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly one");
		assertThatThrownBy(() -> tool.invoke(Map.of("id", "12", "externalKey", "k"), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly one");
	}
}
