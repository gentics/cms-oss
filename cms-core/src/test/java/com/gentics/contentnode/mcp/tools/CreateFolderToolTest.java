package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;

/**
 * Unit tests for {@link CreateFolderTool}. Creating a real folder is covered by the live check.
 */
public class CreateFolderToolTest {
	private static final Map<String, Object> VALID = Map.of("parentFolderId", 57, "name", "Campaigns");

	private final CreateFolderTool tool = new CreateFolderTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "create_folder", "Create folder", "parentFolderId", "name");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, VALID);
		assertAccepted(tool, with("publishDir", "campaigns", "description", "Spring", "idempotencyKey", "k1"));
		assertRejected(tool, Map.of("name", "Campaigns"));
		assertRejected(tool, with("parentFolderId", 0));
		assertRejected(tool, with("name", ""));
		assertRejected(tool, with("description", "x".repeat(256)));
		assertRejected(tool, with("motherId", 57));
	}

	@Test
	public void testRejectsChannel() {
		assertThatThrownBy(() -> tool.invoke(with("nodeId", 2), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("create_folder does not yet support nodeId");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, VALID);
	}

	@Test
	public void testRequest() {
		FolderCreateRequest request = CreateFolderTool.request(with("publishDir", "campaigns"));
		assertThat(request.getMotherId()).isEqualTo("57");
		assertThat(request.getName()).isEqualTo("Campaigns");
		assertThat(request.getPublishDir()).isEqualTo("campaigns");
		assertThat(request.getDescription()).isNull();
		assertThat(request.isFailOnDuplicate()).isTrue();
		assertThat(request.getNodeId()).isNull();

		assertThatThrownBy(() -> CreateFolderTool.request(Map.of("parentFolderId", 57)))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
		assertThatThrownBy(() -> CreateFolderTool.request(Map.of("name", "Campaigns")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("parentFolderId");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResult() {
		Folder folder = new Folder();
		folder.setId(58);
		folder.setName("Campaigns");
		folder.setMotherId(57);
		folder.setNodeId(1);
		folder.setPublishDir("/campaigns/");

		Map<String, Object> json = assertValidOutput(tool,
				new CreateFolderTool.Result(CreateFolderTool.CreatedFolder.of(folder), true));
		assertThat(json).containsEntry("created", true);
		Map<String, Object> created = (Map<String, Object>) json.get("folder");
		assertThat(created).containsEntry("parentFolderId", 57).containsEntry("publishDir", "/campaigns/")
				.doesNotContainKey("description");
		assertThat((Map<String, Object>) created.get("ref")).containsEntry("type", "folder").containsEntry("id", 58)
				.containsEntry("nodeId", 1);
	}

	/**
	 * Get the valid arguments with additional ones
	 * @param keyValues additional arguments, alternating name and value
	 * @return arguments
	 */
	private static Map<String, Object> with(Object... keyValues) {
		Map<String, Object> arguments = new HashMap<>(VALID);
		for (int i = 0; i < keyValues.length; i += 2) {
			arguments.put((String) keyValues[i], keyValues[i + 1]);
		}
		return arguments;
	}
}
