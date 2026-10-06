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

import com.gentics.contentnode.mcp.model.PageInfo;
import com.gentics.contentnode.mcp.model.VersionInfo;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.PageVersion;

/**
 * Unit tests for {@link RestorePageVersionTool}. Restoring a real page is covered by the live check.
 */
public class RestorePageVersionToolTest {
	private final RestorePageVersionTool tool = new RestorePageVersionTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "restore_page_version", "Restore page version", "pageId", "version");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("pageId", 4711, "version", 1700000000));
		assertRejected(tool, Map.of("pageId", 4711));
		assertRejected(tool, Map.of("pageId", 4711, "version", 0));
		assertRejected(tool, Map.of("pageId", 4711, "version", "2.0"));
	}

	@Test
	public void testRejectsChannel() {
		assertThatThrownBy(() -> tool.invoke(Map.of("pageId", 4711, "version", 1, "nodeId", 2), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("support nodeId");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("pageId", 4711, "version", 1700000000));
	}

	@Test
	public void testFind() {
		List<PageVersion> versions = List.of(version("1.0", 1000), version("2.0", 2000));

		assertThat(RestorePageVersionTool.find(versions, 2000).getNumber()).isEqualTo("2.0");
		assertThat(RestorePageVersionTool.find(versions, 1500)).isNull();
		assertThat(RestorePageVersionTool.find(null, 1000)).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResult() {
		Page page = new Page();
		page.setId(4711);

		Map<String, Object> json = assertValidOutput(tool,
				new RestorePageVersionTool.Result(PageInfo.of(page), VersionInfo.of(version("1.0", 1000))));
		assertThat((Map<String, Object>) json.get("restoredVersion")).containsEntry("number", "1.0")
				.containsEntry("timestamp", 1000);
	}

	private static PageVersion version(String number, int timestamp) {
		PageVersion version = new PageVersion();
		version.setNumber(number);
		version.setTimestamp(timestamp);
		return version;
	}
}
