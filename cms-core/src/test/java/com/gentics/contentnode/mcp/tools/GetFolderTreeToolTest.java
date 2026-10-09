package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.rest.model.Folder;

/**
 * Unit tests for {@link GetFolderTreeTool}. Loading a real tree is covered by the live check.
 */
public class GetFolderTreeToolTest {
	private final GetFolderTreeTool tool = new GetFolderTreeTool();

	/**
	 * Folder 1 has subfolders 10 and 11, folder 10 has 100, folder 100 has 1000
	 */
	private final GetFolderTreeTool.Subfolders subfolders = id -> switch (id) {
	case 1 -> List.of(folder(10, true), folder(11, false));
	case 10 -> List.of(folder(100, true));
	case 100 -> List.of(folder(1000, false));
	default -> List.of();
	};

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_folder_tree", "Get folder tree", "nodeId");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("nodeId", 1, "folderId", 57, "depth", 5, "includeCounts", true));
		assertRejected(tool, Map.of("folderId", 57));
		assertRejected(tool, Map.of("nodeId", 1, "depth", 6));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("nodeId", 1));
	}

	@Test
	public void testTwoLevels() throws Exception {
		GetFolderTreeTool.Result result = GetFolderTreeTool.tree(ObjectRef.of(Type.FOLDER, 1), 1, 2, subfolders,
				null);

		assertThat(result.tree()).extracting(node -> node.ref().id()).containsExactly(10, 11);
		GetFolderTreeTool.FolderTreeNode ten = result.tree().get(0);
		assertThat(ten.hasMoreChildren()).isFalse();
		assertThat(ten.children()).extracting(node -> node.ref().id()).containsExactly(100);
		assertThat(ten.children().get(0).hasMoreChildren()).isTrue();
		assertThat(ten.children().get(0).children()).isEmpty();
		assertThat(ten.pageCount()).isNull();
		assertThat(result.truncated()).isFalse();
		assertValidOutput(tool, result);
	}

	@Test
	public void testDepthOneWithCounts() throws Exception {
		List<Integer> counted = new ArrayList<>();
		GetFolderTreeTool.Result result = GetFolderTreeTool.tree(ObjectRef.of(Type.FOLDER, 1), 1, 1, subfolders,
				id -> {
					counted.add(id);
					return id / 10;
				});

		assertThat(result.tree()).extracting(GetFolderTreeTool.FolderTreeNode::hasMoreChildren).containsExactly(true,
				false);
		assertThat(result.tree()).extracting(GetFolderTreeTool.FolderTreeNode::pageCount).containsExactly(1, 1);
		assertThat(counted).containsExactly(10, 11);
	}

	@Test
	public void testFolderLimit() throws Exception {
		List<Folder> many = new ArrayList<>();
		for (int i = 0; i < GetFolderTreeTool.MAX_FOLDERS + 1; i++) {
			many.add(folder(10 + i, false));
		}

		GetFolderTreeTool.Result result = GetFolderTreeTool.tree(ObjectRef.of(Type.FOLDER, 1), 1, 1,
				id -> id == 1 ? many : List.of(), null);

		assertThat(result.tree()).hasSize(GetFolderTreeTool.MAX_FOLDERS);
		assertThat(result.truncated()).isTrue();
	}

	private static Folder folder(int id, boolean hasSubfolders) {
		Folder folder = new Folder();
		folder.setId(id);
		folder.setName("Folder " + id);
		folder.setNodeId(1);
		folder.setHasSubfolders(hasSubfolders);
		return folder;
	}
}
