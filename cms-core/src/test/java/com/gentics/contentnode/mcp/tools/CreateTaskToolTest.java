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
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.util.Tasks;
import com.gentics.contentnode.rest.model.task.TaskSource;
import com.gentics.contentnode.rest.model.task.TaskStatus;
import com.gentics.contentnode.rest.model.task.UserTaskModel;

/**
 * Unit tests for {@link CreateTaskTool}. Creating, the upsert by external key and the permission checks are covered
 * by the tests of the task resource and the live check.
 */
public class CreateTaskToolTest {
	private static final Map<String, Object> RELEASED = Map.of("type", "genaixReadyToPublish", "title", "Terms",
			"status", "open", "source", "genaix", "externalKey", "genaix:s:p:release", "genaix",
			Map.of("sessionId", "s", "planId", "p"), "objects",
			List.of(Map.of("type", "page", "id", 42, "nodeId", 1, "name", "Terms")));

	private final CreateTaskTool tool = new CreateTaskTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "create_task", "Create task", "type", "title");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("type", "custom", "title", "Check the imprint"));
		assertAccepted(tool, RELEASED);
		assertAccepted(tool, Map.of("type", "requestConstruct", "title", "New construct", "assignee",
				Map.of("type", "group", "id", 7)));
		assertRejected(tool, Map.of("title", "No type"));
		assertRejected(tool, Map.of("type", "custom", "title", "T", "status", "finished"));
		assertRejected(tool, Map.of("type", "custom", "title", "T", "source", "publishQueue"));
		assertRejected(tool, Map.of("type", "custom", "title", "T", "id", "5"));
		assertRejected(tool, Map.of("type", "custom", "title", "T", "objects", List.of(Map.of("type", "page"))));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, RELEASED);
	}

	@Test
	public void testArgumentsMakeUpTheTask() {
		UserTaskModel task = Tasks.model(RELEASED);
		assertThat(task.getSource()).isEqualTo(TaskSource.genaix);
		assertThat(task.getStatus()).isEqualTo(TaskStatus.open);
		assertThat(task.getExternalKey()).isEqualTo("genaix:s:p:release");
		assertThat(task.getGenaix().getPlanId()).isEqualTo("p");
		assertThat(task.getObjects()).hasSize(1);
		assertThat(task.getObjects().get(0).getNodeId()).isEqualTo(1);
	}

	@Test
	public void testInvalidArgumentsRejectedBeforeAnyWrite() {
		// the arguments are checked before the task resource is touched, which would fail here
		assertThatThrownBy(() -> tool.invoke(Map.of("type", "custom", "title", "T", "source", "linkChecker"),
				Optional.empty())).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("source");
		assertThatThrownBy(() -> tool.invoke(Map.of("type", "custom", "title", "T", "unknown", 1), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("do not make up a task");
	}

	@Test
	public void testOutput() {
		assertValidOutput(tool, new CreateTaskTool.Result(Tasks.model(RELEASED).setId("12")));
	}
}
