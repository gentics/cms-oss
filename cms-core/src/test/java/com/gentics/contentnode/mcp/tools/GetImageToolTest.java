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
import com.gentics.contentnode.rest.model.Image;

/**
 * Unit tests for {@link GetImageTool}. Loading a real image is covered by the live check.
 */
public class GetImageToolTest {
	private final GetImageTool tool = new GetImageTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_image", "Get image", "id");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", 77, "nodeId", 3));
		assertRejected(tool, Map.of("nodeId", 3));
		assertRejected(tool, Map.of("id", "77"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", 77));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResult() {
		Image image = new Image();
		image.setId(77);
		image.setName("logo.png");
		image.setSizeX(640);
		image.setSizeY(480);
		image.setInheritedFromId(3);

		Map<String, Object> json = assertValidOutput(tool, new GetImageTool.Result(FileInfo.of(image, 4)));

		Map<String, Object> info = (Map<String, Object>) json.get("image");
		assertThat(info).containsEntry("width", 640).containsEntry("height", 480);
		assertThat((Map<String, Object>) info.get("ref")).containsEntry("type", "image").containsEntry("nodeId", 4);
	}
}
