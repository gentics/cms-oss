package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.FileInfo;
import com.gentics.contentnode.rest.model.File;

/**
 * Unit tests for {@link GetFileTool}. Loading a real file is covered by the live check.
 */
public class GetFileToolTest {
	private final GetFileTool tool = new GetFileTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_file", "Get file", "id");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", 12));
		assertAccepted(tool, Map.of("id", 12, "nodeId", 3));
		assertRejected(tool, Map.of());
		assertRejected(tool, Map.of("id", 0));
		assertRejected(tool, Map.of("id", 12, "content", true));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", 12));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResult() {
		File file = new File();
		file.setId(12);
		file.setName("report.pdf");
		file.setFileType("application/pdf");
		file.setFileSize(1024);
		file.setFolderId(57);
		file.setInheritedFromId(3);
		file.setLiveUrl("https://www.example.com/report.pdf");
		file.setUrl("/preview/report.pdf");
		file.setEdate(1700000000);

		Map<String, Object> json = assertValidOutput(tool, new GetFileTool.Result(FileInfo.of(file, null)));

		Map<String, Object> info = (Map<String, Object>) json.get("file");
		assertThat(info).containsEntry("fileName", "report.pdf").containsEntry("mimeType", "application/pdf")
				.containsEntry("sizeBytes", 1024).containsEntry("url", "https://www.example.com/report.pdf")
				.containsEntry("edited", 1700000000).doesNotContainKeys("width", "height", "created");
		assertThat((Map<String, Object>) info.get("ref")).containsEntry("type", "file").containsEntry("nodeId", 3);
	}
}
