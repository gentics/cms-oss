package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.PageVersion;
import com.gentics.contentnode.rest.model.User;

/**
 * Unit tests for {@link GetPageVersionsTool}. Loading a real page is covered by the live check.
 */
public class GetPageVersionsToolTest {
	private final GetPageVersionsTool tool = new GetPageVersionsTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_page_versions", "Get page versions", "id");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", 4711, "size", 100));
		assertRejected(tool, Map.of("id", 4711, "size", 101));
		assertRejected(tool, Map.of("id", 4711, "size", 0));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", 4711));
	}

	@Test
	public void testPublishedVersionMarked() {
		GetPageVersionsTool.Result result = GetPageVersionsTool.result(page(), null, 25);

		assertThat(result.versions()).extracting(GetPageVersionsTool.Version::number).containsExactly("2.1", "2.0",
				"1.0");
		assertThat(result.versions()).extracting(GetPageVersionsTool.Version::published).containsExactly(false, true,
				false);
		assertThat(result.publishedVersion().number()).isEqualTo("2.0");
		assertThat(result.currentVersion().number()).isEqualTo("2.1");
		assertThat(result.truncated()).isFalse();
		assertThat(result.ref().nodeId()).isNull();
		assertValidOutput(tool, result);
	}

	@Test
	public void testSize() {
		GetPageVersionsTool.Result result = GetPageVersionsTool.result(page(), 3, 2);

		assertThat(result.versions()).hasSize(2);
		assertThat(result.truncated()).isTrue();
		assertThat(result.ref().nodeId()).isEqualTo(3);
	}

	@Test
	public void testNeverPublished() {
		Page page = page();
		page.setPublishedVersion(null);

		GetPageVersionsTool.Result result = GetPageVersionsTool.result(page, null, 25);

		assertThat(result.versions()).noneMatch(GetPageVersionsTool.Version::published);
		assertThat(assertValidOutput(tool, result)).doesNotContainKey("publishedVersion");
	}

	private static Page page() {
		PageVersion v21 = version("2.1", 3000);
		PageVersion v20 = version("2.0", 2000);
		Page page = new Page();
		page.setId(4711);
		page.setName("Home");
		// oldest first, as the REST page lists them
		page.setVersions(List.of(version("1.0", 1000), v20, v21));
		page.setCurrentVersion(v21);
		page.setPublishedVersion(version("2.0", 2000));
		return page;
	}

	private static PageVersion version(String number, int timestamp) {
		User editor = new User();
		editor.setId(3);
		editor.setLogin("node");
		PageVersion version = new PageVersion();
		version.setNumber(number);
		version.setTimestamp(timestamp);
		version.setEditor(editor);
		return version;
	}
}
