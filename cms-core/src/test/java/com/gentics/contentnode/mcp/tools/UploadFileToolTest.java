package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.FileInfo;
import com.gentics.contentnode.rest.model.Image;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Unit tests for {@link UploadFileTool}. Uploading a real file is covered by the live check.
 */
public class UploadFileToolTest {
	private static final String DATA = Base64.getEncoder().encodeToString("hello".getBytes());

	private final UploadFileTool tool = new UploadFileTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "upload_file", "Upload file", "folderId", "name", "source");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("folderId", 7, "name", "a.txt", "source", Map.of("kind", "base64", "data", DATA)));
		assertAccepted(tool, Map.of("folderId", 7, "name", "a.png", "source", Map.of("kind", "url", "url",
				"https://example.com/a.png")));
		assertRejected(tool, Map.of("folderId", 7, "name", "a.txt"));
		assertRejected(tool, Map.of("folderId", 7, "name", "a.txt", "source", Map.of("kind", "ftp", "data", DATA)));
		assertRejected(tool, Map.of("folderId", 7, "name", "a.txt", "source", Map.of("kind", "base64")));
	}

	@Test
	public void testRejectsChannel() {
		assertThatThrownBy(() -> tool.invoke(Map.of("folderId", 7, "nodeId", 2, "name", "a.txt", "source",
				Map.of("kind", "base64", "data", DATA)), Optional.empty())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("support nodeId");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool,
				Map.of("folderId", 7, "name", "a.txt", "source", Map.of("kind", "base64", "data", DATA)));
	}

	@Test
	public void testDecode() {
		assertThat(UploadFileTool.decode(Map.of("kind", "base64", "data", DATA))).isEqualTo("hello".getBytes());
		assertThat(UploadFileTool.decode(Map.of("kind", "base64", "data", DATA.substring(0, 4) + "\r\n"
				+ DATA.substring(4)))).isEqualTo("hello".getBytes());
		assertThatThrownBy(() -> UploadFileTool.decode(Map.of("kind", "url", "url", "https://example.com/a.png")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("URL sources are not supported");
		assertThatThrownBy(() -> UploadFileTool.decode(Map.of("kind", "base64", "data", "!!!")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not valid base64");
		assertThatThrownBy(() -> UploadFileTool.decode(Map.of("kind", "base64", "data", "")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("0 bytes");
		String oversize = Base64.getEncoder().encodeToString(new byte[UploadFileTool.MAX_BYTES + 1]);
		assertThatThrownBy(() -> UploadFileTool.decode(Map.of("kind", "base64", "data", oversize)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> UploadFileTool.decode(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testFileName() {
		assertThat(UploadFileTool.fileName("logo.png", "image/jpeg")).isEqualTo("logo.png");
		assertThat(UploadFileTool.fileName("logo", "image/png")).isEqualTo("logo.png");
		assertThat(UploadFileTool.fileName("notes", "text/plain; charset=UTF-8")).isEqualTo("notes.txt");
		assertThat(UploadFileTool.fileName("data", "application/x-unknown")).isEqualTo("data");
		assertThat(UploadFileTool.fileName("data", null)).isEqualTo("data");
		assertThat(UploadFileTool.fileName(".htaccess", "text/plain")).isEqualTo(".htaccess.txt");
	}

	@Test
	public void testRequestOnlyServesInputStream() throws Exception {
		HttpServletRequest request = UploadFileTool.request("hello".getBytes());

		assertThat(request.getInputStream().readAllBytes()).isEqualTo("hello".getBytes());
		assertThatThrownBy(() -> request.getParameter("folderId")).isInstanceOf(UnsupportedOperationException.class)
				.hasMessageContaining("getParameter");
		assertThatThrownBy(request::getContentType).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResult() {
		Image image = new Image();
		image.setId(12);
		image.setName("logo.png");
		image.setFileType("image/png");
		image.setFileSize(10240);
		image.setSizeX(64);
		image.setSizeY(32);

		Map<String, Object> json = assertValidOutput(tool,
				new UploadFileTool.Result(new UploadFileTool.UploadedFile(FileInfo.of(image, null), true), true));
		assertThat((Map<String, Object>) json.get("file")).containsEntry("isImage", true).containsEntry("width", 64)
				.containsEntry("sizeBytes", 10240).containsEntry("fileName", "logo.png");
		assertThat(json).containsEntry("created", true);
	}
}
