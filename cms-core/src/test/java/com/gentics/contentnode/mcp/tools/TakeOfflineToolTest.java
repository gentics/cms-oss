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

import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.QueuedTimeManagement;
import com.gentics.contentnode.rest.model.TimeManagement;

/**
 * Unit tests for {@link TakeOfflineTool}. Taking a real page offline is covered by the live check.
 */
public class TakeOfflineToolTest {
	private final TakeOfflineTool tool = new TakeOfflineTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "take_offline", "Take offline", "pageId");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("pageId", 4711));
		assertAccepted(tool, Map.of("pageId", 4711, "at", 1800000000, "allLanguages", true));
		assertRejected(tool, Map.of("pageId", 4711, "at", -1));
		assertRejected(tool, Map.of("pageId", 4711, "message", "x"));
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
	public void testOffline() {
		Page page = PublishPageToolTest.page(false);
		page.setTimeManagement(new TimeManagement());

		TakeOfflineTool.Result result = TakeOfflineTool.result(page, 0);

		assertThat(result).extracting(TakeOfflineTool.Result::result, TakeOfflineTool.Result::online,
				TakeOfflineTool.Result::offlineAt).containsExactly("offline", false, null);
		assertValidOutput(tool, result);
	}

	@Test
	public void testScheduled() {
		Page page = PublishPageToolTest.page(true);
		page.setTimeManagement(new TimeManagement().setOfflineAt(1800000000));

		TakeOfflineTool.Result result = TakeOfflineTool.result(page, 1800000000);

		assertThat(result).extracting(TakeOfflineTool.Result::result, TakeOfflineTool.Result::online,
				TakeOfflineTool.Result::offlineAt).containsExactly("scheduled", true, 1800000000);
		assertValidOutput(tool, result);
	}

	@Test
	public void testQueued() {
		Page page = PublishPageToolTest.page(true);
		page.setTimeManagement(new TimeManagement().setQueuedOffline(new QueuedTimeManagement().setAt(0)));

		TakeOfflineTool.Result result = TakeOfflineTool.result(page, 0);

		assertThat(result).extracting(TakeOfflineTool.Result::result, TakeOfflineTool.Result::online,
				TakeOfflineTool.Result::offlineAt).containsExactly("queued_for_approval", true, null);
		assertValidOutput(tool, result);
	}
}
