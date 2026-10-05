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

import org.assertj.core.groups.Tuple;
import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.QueuedTimeManagement;
import com.gentics.contentnode.rest.model.TimeManagement;
import com.gentics.contentnode.rest.model.Workflow;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;

/**
 * Unit tests for {@link PublishPageTool}. Publishing a real page is covered by the live check.
 */
public class PublishPageToolTest {
	private final PublishPageTool tool = new PublishPageTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "publish_page", "Publish page", "pageId");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("pageId", 4711));
		assertAccepted(tool, Map.of("pageId", 4711, "at", 1800000000, "allLanguages", true, "message", "please",
				"keepVersion", true));
		assertRejected(tool, Map.of("pageId", 4711, "at", -1));
		assertRejected(tool, Map.of("pageId", 4711, "message", "x".repeat(2001)));
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
	public void testPublished() {
		Page page = page(true);
		page.setTimeManagement(new TimeManagement());

		PublishPageTool.Result result = PublishPageTool.result(page, 0, List.of("published"));

		assertThat(result.result()).isEqualTo("published");
		assertThat(result.online()).isTrue();
		assertThat(result.publishAt()).isNull();
		assertThat(result.approval()).isNull();
		assertThat(result.messages()).containsExactly("published");
		assertValidOutput(tool, result);
	}

	@Test
	public void testScheduled() {
		Page page = page(false);
		page.setTimeManagement(new TimeManagement().setAt(1800000000));

		PublishPageTool.Result result = PublishPageTool.result(page, 1800000000, List.of());

		assertThat(result.result()).isEqualTo("scheduled");
		assertThat(result.online()).isFalse();
		assertThat(result.publishAt()).isEqualTo(1800000000);
		assertValidOutput(tool, result);
	}

	@Test
	public void testQueued() {
		Page page = page(false);
		page.setQueued(true);
		page.setTimeManagement(new TimeManagement().setQueuedPublish(new QueuedTimeManagement().setAt(0)));

		PublishPageTool.Result result = PublishPageTool.result(page, 0, List.of());

		assertThat(result.result()).isEqualTo("queued_for_approval");
		assertThat(result.publishAt()).isNull();
		assertThat(result.approval()).isEqualTo(new PublishPageTool.Approval(true, null, null));
		assertValidOutput(tool, result);
	}

	@Test
	public void testQueuedTimed() {
		Page page = page(true);
		page.setTimeManagement(new TimeManagement().setQueuedPublish(new QueuedTimeManagement().setAt(1800000000)));

		PublishPageTool.Result result = PublishPageTool.result(page, 1800000000, List.of());

		assertThat(result.result()).isEqualTo("queued_for_approval");
		assertThat(result.publishAt()).isEqualTo(1800000000);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testWorkflow() {
		Group editors = new Group();
		editors.setId(5);
		editors.setName("Editors");
		Workflow workflow = new Workflow();
		workflow.setGroups(List.of(editors));
		workflow.setMessage("please check");
		Page page = page(false);
		page.setWorkflow(workflow);

		PublishPageTool.Result result = PublishPageTool.result(page, 0, List.of());

		assertThat(result.result()).isEqualTo("queued_for_approval");
		assertThat(result.approval().message()).isEqualTo("please check");
		assertThat(result.approval().groups()).extracting(ObjectRef::type, ObjectRef::id, ObjectRef::name)
				.containsExactly(Tuple.tuple(ObjectRef.Type.GROUP, 5, "Editors"));
		Map<String, Object> json = assertValidOutput(tool, result);
		assertThat((Map<String, Object>) json.get("approval")).containsEntry("required", true);
	}

	@Test
	public void testMessages() {
		GenericResponse response = new GenericResponse();
		response.addMessage(new Message(Message.Type.SUCCESS, "The page was published."));

		assertThat(PublishPageTool.messages(response)).containsExactly("The page was published.");
		assertThat(PublishPageTool.messages(new GenericResponse())).isEmpty();
	}

	static Page page(boolean online) {
		Page page = new Page();
		page.setId(4711);
		page.setName("Home");
		page.setOnline(online);
		return page;
	}
}
