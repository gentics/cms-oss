package com.gentics.contentnode.tests.rest;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.request.WastebinSearch;
import com.gentics.contentnode.rest.model.response.FileListResponse;
import com.gentics.contentnode.rest.model.response.FolderListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFileListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFolderListResponse;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.PageListResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FolderListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test the sorting of some FolderResource methods
 */
@RunWith(Parameterized.class)
public class FolderSortingTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	int objectType;

    @Parameters(name = "{index}: Type {0}")
    public static Collection<Object[]> data() {
    	return Arrays.asList(new Object[][] {
        	{ Folder.TYPE_FOLDER }, { Page.TYPE_PAGE }, { File.TYPE_FILE }
        });
    }

    public FolderSortingTest(int objectType) {
    	this.objectType = objectType;
    }


	/**
	 * Test the sorting of some FolderResource methods
	 * @throws NodeException
	 * @throws Exception
	 */
	@Test
	public void testSortByDeletedAtAndDeleted() throws NodeException, Exception {
		Node node;
		NodeObject nodeObject1 = null, nodeObject2 = null, nodeObject3 = null;
		SystemUser user1 = null, user2 = null, user3 = null;
		String tableName = null;

		try (Trx trx = new Trx()) {
			NodePreferences prefs = trx.getTransaction().getNodeConfig().getDefaultPreferences();
			prefs.setFeature(Feature.WASTEBIN.toString().toLowerCase(), true);

			user1 = ContentNodeTestDataUtils.createSystemUser("d", "a", "a@b.cd", "user1", "", new ArrayList<UserGroup>());
			user2 = ContentNodeTestDataUtils.createSystemUser("e", "b", "a@b.cd", "user2", "", new ArrayList<UserGroup>());
			user3 = ContentNodeTestDataUtils.createSystemUser("f", "c", "a@b.cd", "user3", "", new ArrayList<UserGroup>());

			node  = ContentNodeTestDataUtils.createNode();
			Folder rootFolder = node.getFolder();

			switch (this.objectType) {
			case Folder.TYPE_FOLDER:
				nodeObject1 = ContentNodeTestDataUtils.createFolder(rootFolder, "Test object 1");
				nodeObject2 = ContentNodeTestDataUtils.createFolder(rootFolder, "Test object 2");
				nodeObject3 = ContentNodeTestDataUtils.createFolder(rootFolder, "Test object 3");
				tableName = "folder";
				break;
			case Page.TYPE_PAGE:
				nodeObject1 = ContentNodeTestDataUtils.createTemplateAndPage(rootFolder, "Test object 1");
				nodeObject2 = ContentNodeTestDataUtils.createTemplateAndPage(rootFolder, "Test object 2");
				nodeObject3 = ContentNodeTestDataUtils.createTemplateAndPage(rootFolder, "Test object 3");
				tableName = "page";
				break;
			case File.TYPE_FILE:
				nodeObject1 = ContentNodeTestDataUtils.createFile(rootFolder, "Test object 1", "content".getBytes());
				nodeObject2 = ContentNodeTestDataUtils.createFile(rootFolder, "Test object 2", "content".getBytes());
				nodeObject3 = ContentNodeTestDataUtils.createFile(rootFolder, "Test object 3", "content".getBytes());
				tableName = "contentfile";
				break;
			}

			trx.success();
		}

		try (Trx trx = new Trx()) {
			DBUtils.executeUpdate("UPDATE " + tableName + " SET deleted = ?, deletedby = ? WHERE id = ?",
					new Object[] {5, user1.getId(), nodeObject1.getId()});
			DBUtils.executeUpdate("UPDATE " + tableName + " SET deleted = ?, deletedby = ? WHERE id = ?",
					new Object[] {10, user2.getId(), nodeObject2.getId()});
			DBUtils.executeUpdate("UPDATE " + tableName + " SET deleted = ?, deletedby = ? WHERE id = ?",
					new Object[] {15, user3.getId(), nodeObject3.getId()});

			trx.success();
		}

		// The new list endpoints use different comparators for sorting which do not support the deletedat and deletedby
		// fields, so we test them by sorting by the name.
		assertDeletedRestObjectsFromFolderSorting(node.getFolder(), "name", "ASC",
			new Integer[]{ nodeObject1.getId(), nodeObject2.getId(), nodeObject3.getId() });
		assertDeletedRestObjectsFromFolderSorting(node.getFolder(), "name", "DESC",
			new Integer[]{ nodeObject3.getId(), nodeObject2.getId(), nodeObject1.getId() });

		assertDeletedRestObjectsFromFolderSortingLegacy(node.getFolder(), "deletedat", "ASC",
				new Integer[]{ nodeObject1.getId(), nodeObject2.getId(), nodeObject3.getId() });
		assertDeletedRestObjectsFromFolderSortingLegacy(node.getFolder(), "deletedat", "DESC",
				new Integer[]{ nodeObject3.getId(), nodeObject2.getId(), nodeObject1.getId() });

		assertDeletedRestObjectsFromFolderSortingLegacy(node.getFolder(), "deletedby", "ASC",
				new Integer[]{ nodeObject1.getId(), nodeObject2.getId(), nodeObject3.getId() });
		assertDeletedRestObjectsFromFolderSortingLegacy(node.getFolder(), "deletedby", "DESC",
				new Integer[]{ nodeObject3.getId(), nodeObject2.getId(), nodeObject1.getId() });

	}


	@SuppressWarnings("unchecked")
	protected void assertDeletedRestObjectsFromFolderSortingLegacy(Folder folder, String sortField, String sortWay, Integer[] ids) throws Exception {
		List<com.gentics.contentnode.rest.model.ContentNodeItem> deletedObjects = new ArrayList<>();
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(folder.getId().toString());
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean().setSortBy(sortField).setSortOrder(sortWay);
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean().setWastebinSearch(WastebinSearch.include);

		try (Trx trx = new Trx()) {
			FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

			switch (this.objectType) {
			case Folder.TYPE_FOLDER:
				LegacyFolderListResponse folderListResponse = folderResource.getFolders(
					inFolder.folderId,
					new ArrayList<>(),
					false,
					inFolder,
					new FolderListParameterBean(),
					filter,
					sorting,
					paging,
					editable,
					wastebin
				);

				ContentNodeTestUtils.assertResponseCodeOk(folderListResponse);
				deletedObjects = (List<com.gentics.contentnode.rest.model.ContentNodeItem>)(Object)folderListResponse.getFolders();
				break;
			case Page.TYPE_PAGE:
				LegacyPageListResponse pageListResponse = folderResource.getPages(
					inFolder.folderId,
					inFolder,
					new PageListParameterBean(),
					filter,
					sorting,
					paging,
					new PublishableParameterBean(),
					wastebin);

				ContentNodeTestUtils.assertResponseCodeOk(pageListResponse);
				deletedObjects = (List<com.gentics.contentnode.rest.model.ContentNodeItem>)(Object)pageListResponse.getPages();
				break;
			case File.TYPE_FILE:
				LegacyFileListResponse fileListResponse = folderResource.getFiles(
					inFolder.folderId,
					inFolder.clone().setRecursive(true),
					new FileListParameterBean().setUsed(false),
					filter,
					sorting,
					paging,
					editable,
					wastebin);
				ContentNodeTestUtils.assertResponseCodeOk(fileListResponse);
				deletedObjects = (List<com.gentics.contentnode.rest.model.ContentNodeItem>)(Object)fileListResponse.getFiles();
				break;
			}
		}

		for(int i = 0; i < ids.length; i++) {
			assertEquals("Node object ID must match", deletedObjects.get(i).getId(), ids[i]);
		}
	}

	@SuppressWarnings("unchecked")
	protected void assertDeletedRestObjectsFromFolderSorting(Folder folder, String sortField, String sortWay, Integer[] ids) throws Exception {
		List<com.gentics.contentnode.rest.model.ContentNodeItem> deletedObjects = new ArrayList<>();
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(folder.getId().toString());
		FilterParameterBean filter = new FilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean().setSortBy(sortField).setSortOrder(sortWay);
		PagingParameterBean paging = new PagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean().setWastebinSearch(WastebinSearch.include);

		try (Trx trx = new Trx()) {

			switch (this.objectType) {
			case Folder.TYPE_FOLDER:
				FolderListResponse folderListResponse = ContentNodeRESTUtils.getFolderResource().list(
					inFolder,
					new FolderListParameterBean(),
					filter,
					sorting.toSortParameterBean(),
					paging,
					editable,
					wastebin
				);

				ContentNodeTestUtils.assertResponseCodeOk(folderListResponse);
				deletedObjects = (List<com.gentics.contentnode.rest.model.ContentNodeItem>)(Object)folderListResponse.getItems();
				break;
			case Page.TYPE_PAGE:
				PageListResponse pageListResponse = ContentNodeRESTUtils.getPageResource().list(
					inFolder,
					new PageListParameterBean(),
					filter,
					sorting.toSortParameterBean(),
					paging,
					new PublishableParameterBean(),
					wastebin);

				ContentNodeTestUtils.assertResponseCodeOk(pageListResponse);
				deletedObjects = (List<com.gentics.contentnode.rest.model.ContentNodeItem>)(Object)pageListResponse.getItems();
				break;
			case File.TYPE_FILE:
				FileListResponse fileListResponse = ContentNodeRESTUtils.getFileResource().list(
					inFolder.clone().setRecursive(true),
					new FileListParameterBean().setUsed(false),
					filter,
					sorting.toSortParameterBean(),
					paging,
					editable,
					wastebin);
				ContentNodeTestUtils.assertResponseCodeOk(fileListResponse);
				deletedObjects = (List<com.gentics.contentnode.rest.model.ContentNodeItem>)(Object)fileListResponse.getItems();
				break;
			}
		}

		for(int i = 0; i < ids.length; i++) {
			assertEquals("Node object ID must match", deletedObjects.get(i).getId(), ids[i]);
		}
	}
}
