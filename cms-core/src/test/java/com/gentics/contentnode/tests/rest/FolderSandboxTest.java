package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.rest.util.MiscUtils.doSetPermissions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.ContentNodeItem.ItemType;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderListRequest;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.model.request.WastebinSearch;
import com.gentics.contentnode.rest.model.response.FolderListResponse;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.FolderObjectCountResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ImageListResponse;
import com.gentics.contentnode.rest.model.response.ItemListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFileListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFolderListResponse;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.PageListResponse;
import com.gentics.contentnode.rest.model.response.PermBitsResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PermResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FolderListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

/**
 * Tests for getting folders over the REST API
 */
public class FolderSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * ID of the root folder
	 */
	public static int ROOT_FOLDER_ID = 56;

	/**
	 * ID of the node
	 */
	public static int NODE_ID = 14;

	public static int ROOT_FOLDER_ID_2 = 69;

	/**
	 * Test searching for a folder by ID recursively (should only find one folder)
	 * @throws Exception
	 */
	@Test
	public void testSearchFindFolderByDescription() throws Exception {
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID_2)).setRecursive(true);
		FolderListParameterBean folderListParams = new FolderListParameterBean();
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean().setSearch("jUnit Test description");
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderListResponse response = folderResource.list(
			inFolder,
			folderListParams,
			filter.toFilterParameterBean(),
			sorting.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		assertFolders("Check folders", Arrays.asList(new ExpectedFolder("Folder1-1")), response.getItems());

		// Check again with legacy endpoints.
		LegacyFolderListResponse legacyResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParams.recursiveIds,
			folderListParams.addPrivileges,
			inFolder,
			folderListParams,
			filter,
			sorting,
			paging,
			editable,
			wastebin);

		assertEquals("Check response code (legacy)", ResponseCode.OK, legacyResponse.getResponseInfo().getResponseCode());
		assertFolders("Check folders (legacy)", Arrays.asList(new ExpectedFolder("Folder1-1")), legacyResponse.getFolders());
	}

	/**
	 * Test searching for a folder by ID recursively (should only find one folder)
	 * @throws Exception
	 */
	@Test
	public void testSearchFindFolderById() throws Exception {
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID_2)).setRecursive(true);
		FolderListParameterBean folderListParams = new FolderListParameterBean();
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean().setSearch("72");
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderListResponse response = folderResource.list(
			inFolder,
			folderListParams,
			filter.toFilterParameterBean(),
			sorting.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		assertFolders("Check folders", Arrays.asList(new ExpectedFolder("Folder1-1")), response.getItems());

		// Check again with legacy endpoints.
		LegacyFolderListResponse legacyResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParams.recursiveIds,
			folderListParams.addPrivileges,
			inFolder,
			folderListParams,
			filter,
			sorting,
			paging,
			editable,
			wastebin);

		assertEquals("Check response code (legacy)", ResponseCode.OK, legacyResponse.getResponseInfo().getResponseCode());
		assertFolders("Check folders (legacy)", Arrays.asList(new ExpectedFolder("Folder1-1")), legacyResponse.getFolders());
	}

	/**
	 * Test that getting child folders returns them in the correct alphabetical order
	 * @throws Exception
	 */
	@Test
	public void testGetChildrenOrder() throws Exception {
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();
		FolderCreateRequest request = new FolderCreateRequest();
		FolderLoadResponse loadResponse;

		request.setMotherId(Integer.toString(ROOT_FOLDER_ID));

		request.setName("0");
		loadResponse = folderResource.create(request);
		assertEquals("Check create folder response code", ResponseCode.OK, loadResponse.getResponseInfo().getResponseCode());

		request.setName("[");
		loadResponse = folderResource.create(request);
		assertEquals("Check create folder response code", ResponseCode.OK, loadResponse.getResponseInfo().getResponseCode());

		request.setName(" h");
		loadResponse = folderResource.create(request);
		assertEquals("Check create folder response code", ResponseCode.OK, loadResponse.getResponseInfo().getResponseCode());

		request.setName("Ü");
		loadResponse = folderResource.create(request);
		assertEquals("Check create folder response code", ResponseCode.OK, loadResponse.getResponseInfo().getResponseCode());

		request.setName("m");
		loadResponse = folderResource.create(request);
		assertEquals("Check create folder response code", ResponseCode.OK, loadResponse.getResponseInfo().getResponseCode());

		request.setName("Z");
		loadResponse = folderResource.create(request);
		assertEquals("Check create folder response code", ResponseCode.OK, loadResponse.getResponseInfo().getResponseCode());

		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID));
		FolderListParameterBean folderListParams = new FolderListParameterBean();
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderListResponse response = folderResource.list(
			inFolder,
			folderListParams,
			filter.toFilterParameterBean(),
			sorting.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		assertFolders(
			"Check folders",
			Arrays.asList(
				new ExpectedFolder("0"),
				new ExpectedFolder("Folder1"),
				new ExpectedFolder("Folder2"),
				new ExpectedFolder("Folder3"),
				new ExpectedFolder("h"),
				new ExpectedFolder("m"),
				new ExpectedFolder("Ü"),
				new ExpectedFolder("Z"),
				new ExpectedFolder("[")),
			response.getItems());

		// Check again with legacy endpoints.
		LegacyFolderListResponse legacyResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParams.recursiveIds,
			folderListParams.addPrivileges,
			inFolder,
			folderListParams,
			filter,
			sorting,
			paging,
			editable,
			wastebin);

		assertEquals("Check response code (legacy)", ResponseCode.OK, legacyResponse.getResponseInfo().getResponseCode());
		assertFolders(
			"Check folders (legacy)",
			Arrays.asList(
				new ExpectedFolder("0"),
				new ExpectedFolder("Folder1"),
				new ExpectedFolder("Folder2"),
				new ExpectedFolder("Folder3"),
				new ExpectedFolder("h"),
				new ExpectedFolder("m"),
				new ExpectedFolder("Ü"),
				new ExpectedFolder("Z"),
				new ExpectedFolder("[")),
			legacyResponse.getFolders());
	}

	@Test
	public void testGetItems() throws Exception {
		int count = 10;
		// Prepare some test data
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Folder rootFolder = t.getObject(com.gentics.contentnode.object.Folder.class, ROOT_FOLDER_ID);
		Template template = ContentNodeTestDataUtils.createTemplate(rootFolder, "testTemplate");
		for (int i = 0; i < count; i++) {
			ContentNodeTestDataUtils.createPage(rootFolder, template, "test_" + i + ".html");
		}

		InFolderParameterBean inFolder = new InFolderParameterBean();
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		PublishableParameterBean publishParams = new PublishableParameterBean();
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

		ItemListResponse response = folderResource.getItems(
			Integer.toString(ROOT_FOLDER_ID),
			Arrays.asList(ItemType.page),
			null,
			false,
			false,
			null,
			false,
			inFolder,
			filter,
			sorting,
			paging,
			publishParams);

		ContentNodeTestUtils.assertResponseCodeOk(response);
		assertEquals(count, response.getNumItems().intValue());
	}

	@Test
	public void testGetPublishablePages() throws Exception {
		int count = 10;
		// Prepare some test data
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Folder rootFolder = t.getObject(com.gentics.contentnode.object.Folder.class, ROOT_FOLDER_ID);
		Template template = ContentNodeTestDataUtils.createTemplate(rootFolder, "testTemplate");
		for (int i = 0; i < count; i++) {
			ContentNodeTestDataUtils.createPage(rootFolder, template, "test_" + i + ".html");
		}

		SystemUser systemUser = null;
		UserGroup userGroup = null;
		try (Trx tx = new Trx()) {
			// Setup test user
			userGroup = ContentNodeTestDataUtils.createUserGroup("test group", ContentNodeTestDataUtils.NODE_GROUP_ID);
			systemUser = ContentNodeTestDataUtils.createSystemUser("Tester", "Tester", "", "Tester", "Tester", Arrays.asList(userGroup));

			// Grant permissions to the user
			PermHandler.setPermissions(Node.TYPE_NODE, ROOT_FOLDER_ID, Arrays.asList(userGroup), new Permission(PermHandler.FULL_PERM).toString());
			PermHandler.setPermissions(com.gentics.contentnode.object.Folder.TYPE_FOLDER, ROOT_FOLDER_ID, Arrays.asList(userGroup),
					new Permission(PermHandler.FULL_PERM).toString());
			tx.success();
		}

		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID));
		PageListParameterBean pageListParams = new PageListParameterBean();
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		PublishableParameterBean publishParams = new PublishableParameterBean();
		WastebinParameterBean wastebinParams = new WastebinParameterBean();
		PageResource pageResource = ContentNodeRESTUtils.getPageResource();
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

		try (Trx tx = new Trx(null, systemUser.getId())) {
			PageListResponse response = pageResource.list(
				inFolder,
				pageListParams,
				filter.toFilterParameterBean(),
				sorting.toSortParameterBean(),
				paging.toPagingParameterBean(),
				publishParams,
				wastebinParams);

			ContentNodeTestUtils.assertResponseCodeOk(response);
			assertEquals(count, response.getNumItems());

			// Check again with legacy endpoints.
			LegacyPageListResponse legacyResponse = folderResource.getPages(
				inFolder.folderId,
				inFolder,
				pageListParams,
				filter,
				sorting,
				paging,
				publishParams,
				wastebinParams);

			ContentNodeTestUtils.assertResponseCodeOk(legacyResponse);
			assertEquals(count, legacyResponse.getNumItems().intValue());
		}

		// Create an additional subfolder with some additional pages
		com.gentics.contentnode.object.Folder subFolder;
		try (Trx tx = new Trx(null, 1)) {
			subFolder = ContentNodeTestDataUtils.createFolder(rootFolder, "subFolder");
			for (int i = 0; i < count; i++) {
				ContentNodeTestDataUtils.createPage(subFolder, template, "test_" + i + ".html");
			}
		}

		// Assert that also the pages of the subfolders will be found
		try (Trx tx = new Trx(null, systemUser.getId())) {
			PageListResponse response = pageResource.list(
				inFolder.clone().setRecursive(true),
				pageListParams,
				filter.toFilterParameterBean(),
				sorting.toSortParameterBean(),
				paging.toPagingParameterBean(),
				publishParams,
				wastebinParams);

			// Check again with legacy endpoints.
			LegacyPageListResponse legacyResponse = folderResource.getPages(
				inFolder.folderId,
				inFolder.clone().setRecursive(true),
				pageListParams,
				filter,
				sorting,
				paging,
				publishParams,
				wastebinParams);

			ContentNodeTestUtils.assertResponseCodeOk(legacyResponse);
			assertEquals(2 * count, legacyResponse.getNumItems().intValue());
		}

		// Assert that the result does not change even if we filter for
		// publishable pages
		try (Trx tx = new Trx(null, systemUser.getId())) {
			PageListResponse response = pageResource.list(
				inFolder.clone().setRecursive(true),
				pageListParams.clone().setPermission(Arrays.asList(com.gentics.contentnode.rest.model.request.Permission.publish)),
				filter.toFilterParameterBean(),
				sorting.toSortParameterBean(),
				paging.toPagingParameterBean(),
				publishParams,
				wastebinParams);

			ContentNodeTestUtils.assertResponseCodeOk(response);
			assertEquals(2 * count, response.getNumItems());

			// Check again with legacy endpoints.
			LegacyPageListResponse legacyResponse = folderResource.getPages(
				inFolder.folderId,
				inFolder.clone().setRecursive(true),
				pageListParams.clone().setPermission(Arrays.asList(com.gentics.contentnode.rest.model.request.Permission.publish)),
				filter,
				sorting,
				paging,
				publishParams,
				wastebinParams);

			ContentNodeTestUtils.assertResponseCodeOk(legacyResponse);
			assertEquals(2 * count, legacyResponse.getNumItems().intValue());
		}

		// Revoke the permission on the subfolder
		try (Trx tx = new Trx(null, 1)) {
			PermHandler.setPermissions(com.gentics.contentnode.object.Folder.TYPE_FOLDER, subFolder.getId(), Arrays.asList(userGroup),
					new Permission(PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_VIEW).toString());
			tx.success();
		}

		// Assert that only the pages from the root folder were retrieved
		try (Trx tx = new Trx(null, systemUser.getId())) {
			PageListResponse response = pageResource.list(
				inFolder.clone().setRecursive(true),
				pageListParams.clone().setPermission(Arrays.asList(com.gentics.contentnode.rest.model.request.Permission.publish)),
				filter.toFilterParameterBean(),
				sorting.toSortParameterBean(),
				paging.toPagingParameterBean(),
				publishParams,
				wastebinParams);

			ContentNodeTestUtils.assertResponseCodeOk(response);
			assertEquals(count, response.getNumItems());

			// Check again with legacy endpoints.
			LegacyPageListResponse legacyResponse = folderResource.getPages(
				inFolder.folderId,
				inFolder.clone().setRecursive(true),
				pageListParams.clone().setPermission(Arrays.asList(com.gentics.contentnode.rest.model.request.Permission.publish)),
				filter,
				sorting,
				paging,
				publishParams,
				wastebinParams);

			ContentNodeTestUtils.assertResponseCodeOk(legacyResponse);
			assertEquals(count, legacyResponse.getNumItems().intValue());
		}

	}

	/**
	 * Test getting children (not recursive)
	 * @throws Exception
	 */
	@Test
	public void testGetChildrenNonRecursive() throws Exception {
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID));
		FolderListParameterBean folderListParams = new FolderListParameterBean();
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

		FolderListResponse response = folderResource.list(
			inFolder,
			folderListParams,
			filter.toFilterParameterBean(),
			sorting.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		List<ExpectedFolder> expectedFolders = Arrays.asList(new ExpectedFolder("Folder1"), new ExpectedFolder("Folder2"), new ExpectedFolder("Folder3"));
		assertFolders("Check folders", expectedFolders, response.getItems());

		// Check again with legacy endpoints.
		LegacyFolderListResponse legacyResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParams.recursiveIds,
			folderListParams.addPrivileges,
			inFolder,
			folderListParams,
			filter,
			sorting,
			paging,
			editable,
			wastebin);

		assertEquals("Check response code (legacy)", ResponseCode.OK, legacyResponse.getResponseInfo().getResponseCode());
		assertFolders("Check folders (legacy)", expectedFolders, legacyResponse.getFolders());
	}

	/**
	 * Test getting children recursively
	 * @throws Exception
	 */
	@Test
	public void testGetChilrenRecursive() throws Exception {
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID)).setRecursive(true);
		FolderListParameterBean folderListParams = new FolderListParameterBean();
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();
		List<ExpectedFolder> expectedFolders = Arrays.asList(
			new ExpectedFolder("Folder1"), new ExpectedFolder("Folder1-1"), new ExpectedFolder("Folder1-2"), new ExpectedFolder("Folder1-3"),
			new ExpectedFolder("Folder2"), new ExpectedFolder("Folder2-1"), new ExpectedFolder("Folder2-2"), new ExpectedFolder("Folder2-3"),
			new ExpectedFolder("Folder3"), new ExpectedFolder("Folder3-1"), new ExpectedFolder("Folder3-2"), new ExpectedFolder("Folder3-3"));

		FolderListResponse response = folderResource.list(
			inFolder,
			folderListParams,
			filter.toFilterParameterBean(),
			sorting.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		assertFolders("Check folders", expectedFolders, response.getItems());

		// Check again with legacy endpoints.
		LegacyFolderListResponse legacyResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParams.recursiveIds,
			folderListParams.addPrivileges,
			inFolder,
			folderListParams,
			filter,
			sorting,
			paging,
			editable,
			wastebin);

		assertEquals("Check response code (legacy)", ResponseCode.OK, legacyResponse.getResponseInfo().getResponseCode());
		assertFolders("Check folders (legacy)", expectedFolders, legacyResponse.getFolders());
	}

	/**
	 * Test getting selective children recursively
	 * @throws Exception
	 */
	@Test
	public void testGetChildrenSelectiveRecursive() throws Exception {
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID)).setRecursive(true);
		FolderListParameterBean folderListParams = new FolderListParameterBean().setRecursiveIds(Arrays.asList("59"));
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();
		List<ExpectedFolder> expectedFolders = Arrays.asList(new ExpectedFolder("Folder1"), new ExpectedFolder("Folder2"), new ExpectedFolder("Folder2-1"), new ExpectedFolder("Folder2-2"),
			new ExpectedFolder("Folder2-3"), new ExpectedFolder("Folder3"));

		FolderListResponse response = folderResource.list(
			inFolder,
			folderListParams,
			filter.toFilterParameterBean(),
			sorting.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		assertFolders("Check folders", expectedFolders, response.getItems());

		// Check again with legacy endpoints.
		LegacyFolderListResponse legacyResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParams.recursiveIds,
			folderListParams.addPrivileges,
			inFolder,
			folderListParams,
			filter,
			sorting,
			paging,
			editable,
			wastebin);

		assertEquals("Check response code (legacy)", ResponseCode.OK, legacyResponse.getResponseInfo().getResponseCode());
		assertFolders("Check folders (legacy)", expectedFolders, legacyResponse.getFolders());
	}

	/**
	 * Test getting a folder tree
	 * @throws Exception
	 */
	@Test
	public void testGetFolderTree() throws Exception {
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID)).setRecursive(true);
		FolderListParameterBean folderListParams = new FolderListParameterBean().setTree(true);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

		FolderListResponse response = folderResource.list(
			inFolder,
			folderListParams,
			filter.toFilterParameterBean(),
			sorting.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertGetFolderTreeResponse(response);

		// Check again with legacy endpoints.
		LegacyFolderListResponse legacyResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParams.recursiveIds,
			folderListParams.addPrivileges,
			inFolder,
			folderListParams,
			filter,
			sorting,
			paging,
			editable,
			wastebin);

		assertGetFolderTreeResponse(legacyResponse);

		LegacyFolderListResponse responsePost = folderResource.getFolders(createFolderListRequest(Integer.toString(ROOT_FOLDER_ID), null, 0, -1, true, "name", "asc", false, null, null, null, 0,
				0, 0, 0, true, null, false));

		assertGetFolderTreeResponse(responsePost);
	}

	/**
	 * Test getting a selective folder tree
	 * @throws Exception
	 */
	@Test
	public void testGetSelectiveFolderTree() throws Exception {
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID)).setRecursive(true);
		FolderListParameterBean folderListParams = new FolderListParameterBean().setTree(true).setRecursiveIds(Arrays.asList("57", "58"));
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

		FolderListResponse response = folderResource.list(
			inFolder,
			folderListParams,
			filter.toFilterParameterBean(),
			sorting.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertResponseGetSelectiveFolderTree(response);

		// Check again with legacy endpoints.
		LegacyFolderListResponse legacyResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParams.recursiveIds,
			folderListParams.addPrivileges,
			inFolder,
			folderListParams,
			filter,
			sorting,
			paging,
			editable,
			wastebin);

		assertResponseGetSelectiveFolderTree(legacyResponse);

		LegacyFolderListResponse responsePost = folderResource.getFolders(createFolderListRequest(Integer.toString(ROOT_FOLDER_ID), null, 0, -1, true, "name", "asc", false, null, null, null, 0,
			0, 0, 0, true, Arrays.asList("57", "58"), false));

		assertResponseGetSelectiveFolderTree(responsePost);
	}

	/**
	 * Test getting a selective folder tree with nodeId/folderId
	 * @throws Exception
	 */
	@Test
	public void testGetSelectiveFolderTreeWithNodeId() throws Exception {
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID)).setRecursive(true);
		FolderListParameterBean folderListParams = new FolderListParameterBean().setTree(true).setRecursiveIds(Arrays.asList(NODE_ID + "/57", NODE_ID + "/58"));
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		LegacySortParameterBean sorting = new LegacySortParameterBean();
		LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
		EditableParameterBean editable = new EditableParameterBean();
		WastebinParameterBean wastebin = new WastebinParameterBean();
		FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

		FolderListResponse response = folderResource.list(
			inFolder,
			folderListParams,
			filter.toFilterParameterBean(),
			sorting.toSortParameterBean(),
			paging.toPagingParameterBean(),
			editable,
			wastebin);

		assertResponseGetSelectiveFolderTree(response);

		// Check again with legacy endpoints.
		LegacyFolderListResponse legacyResponse = folderResource.getFolders(
			inFolder.folderId,
			folderListParams.recursiveIds,
			folderListParams.addPrivileges,
			inFolder,
			folderListParams,
			filter,
			sorting,
			paging,
			editable,
			wastebin);

		assertResponseGetSelectiveFolderTree(legacyResponse);

		LegacyFolderListResponse responsePost = folderResource.getFolders(createFolderListRequest(Integer.toString(ROOT_FOLDER_ID), null, 0, -1, true, "name", "asc", false, null, null, null, 0,
			0, 0, 0, true, Arrays.asList(NODE_ID + "/57", NODE_ID + "/58"), false));

		assertResponseGetSelectiveFolderTree(responsePost);
	}

	/**
	 * Test getting a selective folder tree, where "deleted" (not existent) folders where requested
	 * @throws Exception
	 */
	@Test
	public void testgetSelectiveFolderTreeWithDeleted() throws Exception {
		String id = Trx.supply(() -> {
				FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

				// first create a new folder in the node
				FolderCreateRequest request = new FolderCreateRequest();

				request.setMotherId(Integer.toString(ROOT_FOLDER_ID));
				request.setName("Delete Me");
				FolderLoadResponse loadResponse = folderResource.create(request);

				assertEquals("Check response code", ResponseCode.OK, loadResponse.getResponseInfo().getResponseCode());

				String deletedFolderId = Integer.toString(loadResponse.getFolder().getId());

				// delete the folder
				assertEquals("Check response code", ResponseCode.OK, folderResource.delete(deletedFolderId, 0, null).getResponseInfo().getResponseCode());

				return deletedFolderId;
			});

		Trx.consume(deletedFolderId -> {
				FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();
				InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID)).setRecursive(true);
				FolderListParameterBean folderListParams = new FolderListParameterBean().setTree(true).setRecursiveIds(Arrays.asList("57", "58", deletedFolderId));
				LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
				LegacySortParameterBean sorting = new LegacySortParameterBean();
				LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
				EditableParameterBean editable = new EditableParameterBean();
				WastebinParameterBean wastebin = new WastebinParameterBean();

				FolderListResponse response = folderResource.list(
					inFolder,
					folderListParams,
					filter.toFilterParameterBean(),
					sorting.toSortParameterBean(),
					paging.toPagingParameterBean(),
					editable,
					wastebin);

				assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
				List<String> deleted = response.getDeleted();

				assertNotNull("Deleted list must exist", deleted);
				assertTrue("Deleted folder must be returned", deleted.contains(deletedFolderId));
				assertEquals("Check number of deleted folder IDs", 1, deleted.size());

				// Check again with legacy endpoints.
				LegacyFolderListResponse legacyResponse = folderResource.getFolders(
					inFolder.folderId,
					folderListParams.recursiveIds,
					folderListParams.addPrivileges,
					inFolder,
					folderListParams,
					filter,
					sorting,
					paging,
					editable,
					wastebin);

				assertEquals("Check response code", ResponseCode.OK, legacyResponse.getResponseInfo().getResponseCode());

				deleted = legacyResponse.getDeleted();

				assertNotNull("Deleted list must exist", deleted);
				assertTrue("Deleted folder must be returned", deleted.contains(deletedFolderId));
				assertEquals("Check number of deleted folder IDs", 1, deleted.size());
			},
			id);
	}

	/**
	 * Test getting a selective folder tree with nodeId, where "deleted" (not existent) folders where requested
	 * @throws Exception
	 */
	@Test
	public void testgetSelectiveFolderTreeWithNodeIdWithDeleted() throws Exception {
		String id = Trx.supply(() -> {
				FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();

				// first create a new folder in the node
				FolderCreateRequest request = new FolderCreateRequest();

				request.setMotherId(Integer.toString(ROOT_FOLDER_ID));
				request.setName("Delete Me");
				FolderLoadResponse loadResponse = folderResource.create(request);

				assertEquals("Check response code", ResponseCode.OK, loadResponse.getResponseInfo().getResponseCode());

				String deletedFolderId = Integer.toString(loadResponse.getFolder().getId());

				// delete the folder
				assertEquals("Check response code", ResponseCode.OK, folderResource.delete(deletedFolderId, 0, null).getResponseInfo().getResponseCode());

				return deletedFolderId;
			});

		Trx.consume(deletedFolderId -> {
				FolderResource folderResource = ContentNodeRESTUtils.getFolderResource();
				InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(ROOT_FOLDER_ID)).setRecursive(true);
				FolderListParameterBean folderListParams = new FolderListParameterBean().setRecursiveIds(Arrays.asList(NODE_ID + "/57", NODE_ID + "/58", NODE_ID + "/" + deletedFolderId));
				LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
				LegacySortParameterBean sorting = new LegacySortParameterBean();
				LegacyPagingParameterBean paging = new LegacyPagingParameterBean();
				EditableParameterBean editable = new EditableParameterBean();
				WastebinParameterBean wastebin = new WastebinParameterBean();

				FolderListResponse response = folderResource.list(
					inFolder,
					folderListParams,
					filter.toFilterParameterBean(),
					sorting.toSortParameterBean(),
					paging.toPagingParameterBean(),
					editable,
					wastebin);

				assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
				List<String> deleted = response.getDeleted();

				assertNotNull("Deleted list must exist", deleted);
				assertTrue("Deleted folder must be returned", deleted.contains(NODE_ID + "/" + deletedFolderId));
				assertEquals("Check number of deleted folder IDs", 1, deleted.size());

				// Check again with legacy endpoints.
				LegacyFolderListResponse legacyResponse = folderResource.getFolders(
					inFolder.folderId,
					folderListParams.recursiveIds,
					folderListParams.addPrivileges,
					inFolder,
					folderListParams,
					filter,
					sorting,
					paging,
					editable,
					wastebin);

				assertEquals("Check response code", ResponseCode.OK, legacyResponse.getResponseInfo().getResponseCode());

				deleted = legacyResponse.getDeleted();

				assertNotNull("Deleted list must exist", deleted);
				assertTrue("Deleted folder must be returned", deleted.contains(NODE_ID + "/" + deletedFolderId));
				assertEquals("Check number of deleted folder IDs", 1, deleted.size());
			},
			id);

	}

	/**
	 * Test getting images from a folder the user has no permissions to view
	 * images
	 *
	 * @throws Exception
	 */
	@Test
	public void testPermissionGetImagesFromFolder() throws Exception {
		Transaction t = testContext.startSystemUserTransaction();

		com.gentics.contentnode.object.Folder rootFolder = t.getObject(com.gentics.contentnode.object.Folder.class, ROOT_FOLDER_ID);
		com.gentics.contentnode.object.Folder permissionTestFolder = Creator.createFolder(rootFolder, "permissionTestFolder", "/somedir");
		t.commit(false);
		com.gentics.contentnode.object.Folder subFolder = Creator.createFolder(permissionTestFolder, "SubFolder", "/somedir/some");
		t.commit(false);

		// create folder and subfolder
		int testFolderId = ObjectTransformer.getInt(permissionTestFolder.getId(), -1);
		int subFolderId = ObjectTransformer.getInt(subFolder.getId(), -1);

		// put a testimage in each folder
		ImageFile image1 = t.createObject(ImageFile.class);
		image1.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		image1.setName("blume1.jpg");
		image1.setFolderId(testFolderId);
		image1.setForceOnline(true);
		image1.save();
		t.commit(false);

		ImageFile image2 = t.createObject(ImageFile.class);
		image2.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		image2.setName("blume2.jpg");
		image2.setFolderId(subFolderId);
		image2.setForceOnline(true);
		image2.save();
		t.commit(false);

		UserGroup parentGroup = t.getObject(UserGroup.class, 2);
		UserGroup testGroup = Creator.createUsergroup("testgroup", "", parentGroup);
		t.commit(false);

		// Allow the testgroup to view pages in the testfolder and its subfolder
		SetPermsRequest setPermsRequest = new SetPermsRequest();
		setPermsRequest.setGroupId(ObjectTransformer.getInteger(testGroup.getId(), -1));
		// Set permissions for viewing the folder and viewing pages/files/images
		// in the folder
		setPermsRequest.setPerm(new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString());
		GenericResponse permResponse = doSetPermissions(TypePerms.folder, testFolderId, setPermsRequest);
		assertEquals("Check set permission response code for the testfolder", ResponseCode.OK, permResponse.getResponseInfo().getResponseCode());
		t.commit(false);
		permResponse = doSetPermissions(TypePerms.folder, subFolderId, setPermsRequest);
		assertEquals("Check set permission response code for the subfolder", ResponseCode.OK, permResponse.getResponseInfo().getResponseCode());
		t.commit(false);

		Creator.createUser("test", "test", "Rudi", "Mentaer", "test@example.com", Arrays.asList(testGroup));
		t.commit(false);

		testContext.getContext().startTransaction();
		testContext.getContext().login("test", "test");
		t = testContext.getContext().getTransaction();

		FolderResourceImpl folderResource = (FolderResourceImpl) ContentNodeRESTUtils.getFolderResource();
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(Integer.toString(testFolderId));
		FileListParameterBean fileListParams = new FileListParameterBean().setNodeId(NODE_ID);
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean().setSearch("");
		LegacySortParameterBean sort = new LegacySortParameterBean().setSortBy("name").setSortOrder("asc");
		LegacyPagingParameterBean pagingParams = new LegacyPagingParameterBean();
		ImageResourceImpl imageResource = (ImageResourceImpl) ContentNodeRESTUtils.getImageResource();
		EditableParameterBean editableParams = new EditableParameterBean();
		WastebinParameterBean wastebinParams = new WastebinParameterBean();

		// Now list all images and pages in this folder.
		ImageListResponse imageListResponse = imageResource.list(
			inFolder,
			fileListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			pagingParams.toPagingParameterBean(),
			editableParams,
			wastebinParams);

		assertEquals("Check get images response code", ResponseCode.OK, imageListResponse.getResponseInfo().getResponseCode());
		assertEquals("Check number of results", 1, imageListResponse.getNumItems());

		// Check again with legacy endpoint.
		LegacyFileListResponse legacyFileListResponse = folderResource.getImages(
			inFolder.folderId,
			inFolder,
			fileListParams,
			filter,
			sort,
			pagingParams,
			editableParams,
			wastebinParams);

		assertEquals("Check get images response code", ResponseCode.OK, legacyFileListResponse.getResponseInfo().getResponseCode());
		assertEquals("Check number of results", 1, legacyFileListResponse.getNumItems().intValue());

		FolderObjectCountResponse countResponse = folderResource.getObjectCounts(
			testFolderId,
			NODE_ID,
			null,
			false,
			inFolder,
			wastebinParams.clone().setWastebinSearch(WastebinSearch.exclude));

		assertEquals("Compare number of results with imagecount", countResponse.getImages(), imageListResponse.getNumItems());

		//list recursively
		imageListResponse = imageResource.list(
			inFolder.setRecursive(true),
			fileListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			pagingParams.toPagingParameterBean(),
			editableParams,
			wastebinParams);
		assertEquals("Check get images response code", ResponseCode.OK, imageListResponse.getResponseInfo().getResponseCode());
		assertEquals("Check number of results for recursive image search", 2, imageListResponse.getNumItems());

		for (File file : imageListResponse.getItems()){
			assertEquals("Check mimetype", file.getFileType(), "image/jpeg");
			assertEquals("Object has to be of type image", file.getTypeId().intValue(), ContentFile.TYPE_IMAGE);
			assertEquals("Object has to be of type image", file.getType(), ItemType.image);
		}

		// Check again with legacy endpoint
		legacyFileListResponse = folderResource.getImages(
			inFolder.folderId,
			inFolder.setRecursive(true),
			fileListParams,
			filter,
			sort,
			pagingParams,
			editableParams,
			wastebinParams);
		assertEquals("Check get images response code", ResponseCode.OK, legacyFileListResponse.getResponseInfo().getResponseCode());
		assertEquals("Check number of results for recursive image search", 2, legacyFileListResponse.getNumItems().intValue());

		for (File file : legacyFileListResponse.getFiles()){
			assertEquals("Check mimetype", file.getFileType(), "image/jpeg");
			assertEquals("Object has to be of type image", file.getTypeId().intValue(), ContentFile.TYPE_IMAGE);
			assertEquals("Object has to be of type image", file.getType(), ItemType.image);
		}

		// Now retract the page/file/image viewing permission for the testgroup
		// on the testfolder
		testContext.startSystemUserTransaction();
		t = TransactionManager.getCurrentTransaction();
		// Set permissions for viewing the folder and nothing else
		setPermsRequest.setPerm(new Permission(PermHandler.PERM_VIEW).toString());
		permResponse = doSetPermissions(TypePerms.folder, testFolderId, setPermsRequest);
		assertEquals("Check set permission response code", ResponseCode.OK, permResponse.getResponseInfo().getResponseCode());
		t.commit(true);

		testContext.getContext().startTransaction();
		testContext.getContext().login("test", "test");
		t = TransactionManager.getCurrentTransaction();
		imageResource.setTransaction(t);
		folderResource.setTransaction(t);

		// Now list all images and images in this folder again. The testuser
		// should not be able to list images in the testfolder.
		imageListResponse = imageResource.list(
			inFolder.setRecursive(false),
			fileListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			pagingParams.toPagingParameterBean(),
			editableParams,
			wastebinParams);
		assertEquals("Check get images response code", ResponseCode.OK, imageListResponse.getResponseInfo().getResponseCode());
		assertEquals("Check number of results", 0, imageListResponse.getNumItems());

		// Check again with legacy endpoint.
		legacyFileListResponse = folderResource.getImages(
			inFolder.folderId,
			inFolder.setRecursive(false),
			fileListParams,
			filter,
			sort,
			pagingParams,
			editableParams,
			wastebinParams);
		assertEquals("Check get images response code", ResponseCode.OK, legacyFileListResponse.getResponseInfo().getResponseCode());
		assertEquals("Check number of results", 0, legacyFileListResponse.getNumItems().intValue());

		countResponse = folderResource.getObjectCounts(
			testFolderId,
			NODE_ID,
			null,
			false,
			inFolder,
			wastebinParams.setWastebinSearch(WastebinSearch.exclude));
		assertEquals("Compare number of results with imagecount", countResponse.getImages(), imageListResponse.getNumItems());

		//The subfolder still has viewing permissions.
		//So a recursive listing of the testfolder should return one result.
		imageListResponse = imageResource.list(
			inFolder.setRecursive(true),
			fileListParams,
			filter.toFilterParameterBean(),
			sort.toSortParameterBean(),
			pagingParams.toPagingParameterBean(),
			editableParams,
			wastebinParams);

		assertEquals("Check number of results for recursive image search", 1, imageListResponse.getNumItems());

		// Check again with legacy endpoint.
		legacyFileListResponse = folderResource.getImages(
			inFolder.folderId,
			inFolder.setRecursive(true),
			fileListParams,
			filter,
			sort,
			pagingParams,
			editableParams,
			wastebinParams);

		assertEquals("Check number of results for recursive image search", 1, legacyFileListResponse.getNumItems().intValue());
	}

	/**
	 * Tests the response is correct when getting a selective folder tree
	 * @param response
	 * @throws Exception
	 */
	private void assertResponseGetSelectiveFolderTree(FolderListResponse response) throws Exception {
		LegacyFolderListResponse legacyResponse = new LegacyFolderListResponse(null, response.getResponseInfo());

		legacyResponse.setFolders(response.getItems());

		assertResponseGetSelectiveFolderTree(legacyResponse);
	}

	/**
	 * Tests the response is correct when getting a selective folder tree
	 * @param response
	 * @throws Exception
	 */
	private void assertResponseGetSelectiveFolderTree(LegacyFolderListResponse response) throws Exception {
		assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		assertFolders("Check folders",
				Arrays.asList(
				new ExpectedFolder("Folder1", Arrays.asList(new ExpectedFolder("Folder1-1"), new ExpectedFolder("Folder1-2"), new ExpectedFolder("Folder1-3"))),
				new ExpectedFolder("Folder2"),
				new ExpectedFolder("Folder3", Arrays.asList(new ExpectedFolder("Folder3-1"), new ExpectedFolder("Folder3-2"), new ExpectedFolder("Folder3-3")))),
				response.getFolders());
	}

	/**
	 * Asserts the response when getting a folder tree.
	 * @param response
	 * @throws Exception
	 */
	private void assertGetFolderTreeResponse(FolderListResponse response) throws Exception {
		LegacyFolderListResponse legacyResponse = new LegacyFolderListResponse(null, response.getResponseInfo());

		legacyResponse.setFolders(response.getItems());

		assertGetFolderTreeResponse(legacyResponse);
	}

	/**
	 * Asserts the response when getting a folder tree.
	 * @param response
	 * @throws Exception
	 */
	private void assertGetFolderTreeResponse(LegacyFolderListResponse response)
			throws Exception {
		assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		assertFolders("Check folders",
				Arrays.asList(
				new ExpectedFolder("Folder1", Arrays.asList(new ExpectedFolder("Folder1-1"), new ExpectedFolder("Folder1-2"), new ExpectedFolder("Folder1-3"))),
				new ExpectedFolder("Folder2", Arrays.asList(new ExpectedFolder("Folder2-1"), new ExpectedFolder("Folder2-2"), new ExpectedFolder("Folder2-3"))),
				new ExpectedFolder("Folder3", Arrays.asList(new ExpectedFolder("Folder3-1"), new ExpectedFolder("Folder3-2"), new ExpectedFolder("Folder3-3")))),
				response.getFolders());
	}

	/**
	 * Test inheriting roles from parent folder after creating a folder
	 *
	 * @throws NodeException
	 */
	@Test
	public void testRolesInheritance() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create node with language klingon assigned
		ContentLanguage klingon = Creator.createLanguage("Klingon", "tlh");
		int klingonId = (Integer) klingon.getId();
		Node node = Creator.createNode("testnode", "blah", "/", "/", Arrays.asList(new ContentLanguage[] { klingon }));
		int nodeId = (Integer) node.getId();

		// Create a parent folder
		com.gentics.contentnode.object.Folder parentFolder = Creator.createFolder(node.getFolder(),"parentFolder", "/");
		int parentFolderId = (Integer) parentFolder.getId();

		// Create a test group and a test user in the test group
		UserGroup testGroup = Creator.createUsergroup("testtranslator", "", t.getObject(UserGroup.class, 1));
		SystemUser testUser = Creator.createUser("testtranslator", "testtranslator", "rudi", "mentaer", "blah@rg.hh", Arrays.asList(new UserGroup[]{testGroup}));

		// Set the permissions for the parent folder:
		// View permissions, create folder permissions
		String permissions = new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_FOLDER_CREATE).toString();
		PermHandler.setPermissions(com.gentics.contentnode.object.Folder.TYPE_FOLDER, parentFolderId, Arrays.asList(new UserGroup[] { testGroup }), permissions);
		PermHandler.setPermissions(Node.TYPE_NODE, parentFolderId, Arrays.asList(new UserGroup[] { testGroup }), permissions);

		// Set view permissions for klingon for the klingon role on parent folder
		Map<ContentLanguage,String> languagePermissions = new HashMap<ContentLanguage, String>();
		Permission viewPerm = new Permission(PermHandler.ROLE_VIEW);
		languagePermissions.put(klingon, viewPerm.toString());
		int klingonRoleId = Creator.createRole("testrole", languagePermissions, null, null);
		int roleUserGroupId = DBUtils.executeInsert("insert into role_usergroup (role_id, usergroup_id) values (?,?)", new Object[] { klingonRoleId, testGroup.getId() }).get(0);
		DBUtils.executeInsert("insert into role_usergroup_assignment (role_usergroup_id	, obj_type, obj_id) values (?, ?, ?)", new Object[] { roleUserGroupId, com.gentics.contentnode.object.Folder.TYPE_FOLDER, parentFolder.getId() });
		DBUtils.executeInsert("insert into role_usergroup_assignment (role_usergroup_id	, obj_type, obj_id) values (?, ?, ?)", new Object[] { roleUserGroupId, Node.TYPE_NODE, parentFolder.getId() });

		// Re-synchronize Permission store with database contents changed previously
		PermissionStore.initialize(true);

		// Now create a new child folder, that should inherit the role permissions automatically.
		com.gentics.contentnode.object.Folder childFolder = Creator.createFolder(parentFolder, "childFolder", "/");
		int childFolderId = (Integer) childFolder.getId();

		// Commit & restart transaction as test user
		t.commit(false);

		operate(testUser, () -> {
			// Check role inheritance
			PermResourceImpl permResourceImpl = new PermResourceImpl();
			PermBitsResponse parentPermBitResponse = permResourceImpl.getPermissions(TypePerms.folder.name(), parentFolderId, nodeId, Page.TYPE_PAGE, klingonId, false);
			String parentRolePermBitsString = parentPermBitResponse.getRolePerm();
			Assert.assertNotNull("No permissions for klingon role on parent folder", parentRolePermBitsString);
			Assert.assertEquals("Wrong permissions for klingon role on parent folder", viewPerm.toString(), parentRolePermBitsString);

			PermBitsResponse childPermBitResponse = permResourceImpl.getPermissions(TypePerms.folder.name(), childFolderId, nodeId, Page.TYPE_PAGE, klingonId, false);
			String childRolePermBitsString = childPermBitResponse.getRolePerm();
			Assert.assertNotNull("No permissions for klingon role on child folder, permission inheritance failed.", childRolePermBitsString);
			assertEquals("Parent and child role permbits differ.", parentRolePermBitsString, childRolePermBitsString);
		});
	}

	/**
	 * Assert a given list of folders
	 * @param message message
	 * @param expected expected folders
	 * @param actual actual folders
	 * @throws Exception
	 */
	protected static void assertFolders(String message, List<ExpectedFolder> expected, List<Folder> actual) throws Exception {
		if (expected == null) {
			expected = Collections.emptyList();
		}
		if (actual == null) {
			actual = Collections.emptyList();
		}
		assertEquals(message + " - size", expected.size(), actual.size());
		for (int i = 0; i < expected.size(); i++) {
			assertEquals(message + " - #" + i, expected.get(i).getName(), actual.get(i).getName());
			assertFolders(message + " - #" + i + ", children", expected.get(i).getSubfolders(), actual.get(i).getSubfolders());
		}
	}

	/**
	 *  Creates a FolderListRequest
	 * @param id
	 * @param nodeId
	 * @param skipCount
	 * @param maxItems
	 * @param recursive
	 * @param sortBy
	 * @param sortOrder
	 * @param inherited
	 * @param search
	 * @param editor
	 * @param creator
	 * @param editedSince
	 * @param createdSince
	 * @param tree
	 * @param recursiveIds
	 * @param addPrivileges
	 * @return
	 */
	private FolderListRequest createFolderListRequest(String id, Integer nodeId, int skipCount, int maxItems, boolean recursive, String sortBy, String sortOrder, Boolean inherited, String search, String editor, String creator, int editedBefore, int editedSince, int createdBefore, int createdSince, boolean tree, List<String> recursiveIds, boolean addPrivileges) {
		FolderListRequest folerListRequest = new FolderListRequest();

		folerListRequest.setId(id);
		folerListRequest.setNodeId(nodeId);
		folerListRequest.setSkipCount(skipCount);
		folerListRequest.setMaxItems(maxItems);
		folerListRequest.setRecursive(recursive);
		folerListRequest.setSortBy(sortBy);
		folerListRequest.setSortOrder(sortOrder);
		folerListRequest.setInherited(inherited);
		folerListRequest.setSearch(search);
		folerListRequest.setEditor(editor);
		folerListRequest.setCreator(creator);
		folerListRequest.setEditedBefore(editedBefore);
		folerListRequest.setEditedSince(editedSince);
		folerListRequest.setCreatedSince(createdSince);
		folerListRequest.setCreatedBefore(createdBefore);
		folerListRequest.setTree(tree);
		folerListRequest.setRecursiveIds(recursiveIds);
		folerListRequest.setAddPrivileges(addPrivileges);

		return folerListRequest;
	}

	/**
	 * Helper class for expected folders (name and subfolders)
	 */
	public static class ExpectedFolder {

		/**
		 * Folder name
		 */
		protected String name;

		/**
		 * Subfolders
		 */
		protected List<ExpectedFolder> subfolders;

		/**
		 * Create an instance
		 * @param name name
		 */
		public ExpectedFolder(String name) {
			this(name, null);
		}

		/**
		 * Create an instance
		 * @param name name
		 * @param subfolders subfolders
		 */
		public ExpectedFolder(String name, List<ExpectedFolder> subfolders) {
			this.name = name;
			this.subfolders = subfolders;
		}

		/**
		 * Get the name
		 * @return name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Get the subfolders
		 * @return subfolders
		 */
		public List<ExpectedFolder> getSubfolders() {
			return subfolders;
		}
	}
}
