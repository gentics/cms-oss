package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.FolderItem;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Page;

/**
 * Unit tests for {@link ListFolderItemsTool}. Listing a real folder is covered by the live check.
 */
public class ListFolderItemsToolTest {
	private final ListFolderItemsTool tool = new ListFolderItemsTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "list_folder_items", "List folder items", "folderId");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("folderId", 57, "types", List.of("page", "folder"), "recursive", true,
				"filters", Map.of("online", true, "editedSince", 1700000000, "nameContains", "news"), "sort",
				Map.of("field", "pdate", "order", "desc"), "size", 10, "from", 20));
		assertRejected(tool, Map.of("folderId", 57, "types", List.of()));
		assertRejected(tool, Map.of("folderId", 57, "filters", Map.of("published", true)));
		assertRejected(tool, Map.of("folderId", 57, "sort", Map.of("order", "desc")));
		assertRejected(tool, Map.of("folderId", 57, "q", "news"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("folderId", 57));
	}

	@Test
	public void testFormsRejected() {
		assertThatThrownBy(() -> ListFolderItemsTool.Request.of(Map.of("folderId", 57, "types", List.of("form"))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("form");
	}

	@Test
	public void testFilterMapping() {
		ListFolderItemsTool.Request request = ListFolderItemsTool.Request.of(Map.of("folderId", 57, "filters",
				Map.of("online", false, "editedSince", 100, "createdBefore", 200, "publishedSince", 300, "creatorId",
						3, "editorId", 4, "nameContains", " news ")));

		assertThat(request.publishParams().online).isFalse();
		assertThat(request.publishParams().modified).isNull();
		assertThat(request.publishParams().editedSince).isEqualTo(100);
		assertThat(request.publishParams().createdBefore).isEqualTo(200);
		assertThat(request.publishParams().publishedSince).isEqualTo(300);
		assertThat(request.publishParams().creatorIds).containsExactly(3);
		assertThat(request.publishParams().editorIds).containsExactly(4);
		assertThat(request.search()).isEqualTo(" news ");
		assertThat(request.filtersPublishState()).isTrue();
		assertThat(request.types()).containsExactly("page");
		assertThat(request.sortField()).isEqualTo("name");
	}

	@Test
	public void testSortAndSlice() {
		ListFolderItemsTool.Request request = ListFolderItemsTool.Request.of(Map.of("folderId", 57, "types",
				List.of("page", "file", "image", "folder"), "sort", Map.of("field", "edate", "order", "desc"), "size",
				2, "from", 1));

		ListResult<FolderItem> result = ListFolderItemsTool.result(items(), request);

		assertThat(result.total()).isEqualTo(4);
		assertThat(result.nextFrom()).isEqualTo(3);
		assertThat(result.items()).extracting(item -> item.ref().name()).containsExactly("c.png", "d");
		assertValidOutput(tool, result);
	}

	@Test
	public void testItemShapes() {
		ListFolderItemsTool.Request request = ListFolderItemsTool.Request.of(Map.of("folderId", 57));

		List<FolderItem> items = ListFolderItemsTool.result(items(), request).items();

		assertThat(items).extracting(item -> item.ref().type().value()).containsExactly("page", "file", "image",
				"folder");
		assertThat(items.get(0).templateId()).isEqualTo(9);
		assertThat(items.get(0).fileName()).isEqualTo("a.html");
		assertThat(items.get(1).mimeType()).isEqualTo("application/pdf");
		assertThat(items.get(3).online()).isNull();
	}

	@Test
	public void testPdateAndFilenameSort() {
		List<ContentNodeItem> items = items();
		items.sort(ListFolderItemsTool.comparator("pdate"));
		assertThat(items).extracting(ContentNodeItem::getName).startsWith("b.pdf", "c.png", "d");

		items.sort(ListFolderItemsTool.comparator("filename").reversed());
		assertThat(items).extracting(ContentNodeItem::getName).containsExactly("d", "c.png", "b.pdf", "A Page");
	}

	private static List<ContentNodeItem> items() {
		Page page = new Page();
		page.setId(1);
		page.setName("A Page");
		page.setFileName("a.html");
		page.setTemplateId(9);
		page.setEdate(100);
		page.setPdate(500);
		File file = new File();
		file.setId(2);
		file.setName("b.pdf");
		file.setFileType("application/pdf");
		file.setEdate(400);
		Image image = new Image();
		image.setId(3);
		image.setName("c.png");
		image.setEdate(300);
		Folder folder = new Folder();
		folder.setId(4);
		folder.setName("d");
		folder.setEdate(200);
		return new ArrayList<>(List.of(page, file, image, folder));
	}
}
