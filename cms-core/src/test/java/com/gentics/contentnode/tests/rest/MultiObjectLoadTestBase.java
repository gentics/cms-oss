package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.request.MultiFolderLoadRequest;
import com.gentics.contentnode.rest.model.request.MultiObjectLoadRequest;
import com.gentics.contentnode.rest.model.request.MultiPageLoadRequest;
import com.gentics.contentnode.rest.model.response.MultiFileLoadResponse;
import com.gentics.contentnode.rest.model.response.MultiFolderLoadResponse;
import com.gentics.contentnode.rest.model.response.MultiImageLoadResponse;
import com.gentics.contentnode.rest.model.response.MultiPageLoadResponse;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

/**
 * Tests for loading multiple objects via the REST API.
 */
@RunWith(Parameterized.class)
public class MultiObjectLoadTestBase {

	/**
	 * Permissions to be tested against.
	 */
	public enum TestPermission {
		None,
		ReadOnly,
		ReadWrite
	}

	private final static int NODE_GROUP_ID = 2;
	private final static String NO_PERMS_USER_LOGIN = "nopermsuser";
	private final static String RO_USER_LOGIN = "rouser";
	private final static String RW_USER_LOGIN = "rwuser";
	private final static String DEFAULT_PASSWORD = "password";

	private final static int NON_EXISTENT = 0;

	@ClassRule public static DBTestContext testContext = new DBTestContext();

	private static Folder unrestrictedFolder;
	private static Folder restrictedFolder;

	private TestPermission permission;
	private boolean forUpdate;

	@Parameters(name = "{index}: permission {0}, forUpdate {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (TestPermission perm: TestPermission.values()) {
			data.add(new Object[] { perm, true });
			data.add(new Object[] { perm, false });
		}
		return data;
	}
	
	@BeforeClass
	public static void setUpOnce() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = ContentNodeTestDataUtils.createNode();
		Folder rootFolder = node.getFolder();

		t.commit(false);

		UserGroup rwGroup = t.getObject(UserGroup.class, NODE_GROUP_ID);
		UserGroup roGroup = ContentNodeTestDataUtils.createUserGroup(
			"Read only group", ContentNodeTestDataUtils.NODE_GROUP_ID);
		UserGroup noPermsGroup = ContentNodeTestDataUtils.createUserGroup(
			"No permissions group", roGroup.getId());

		ContentNodeTestDataUtils.createSystemUser(
			"Read", "Write", "", RW_USER_LOGIN, DEFAULT_PASSWORD, Arrays.asList(rwGroup));
		ContentNodeTestDataUtils.createSystemUser(
			"Read", "Only", "", RO_USER_LOGIN, DEFAULT_PASSWORD, Arrays.asList(roGroup));
		ContentNodeTestDataUtils.createSystemUser(
			"No", "Permissions", "", NO_PERMS_USER_LOGIN, DEFAULT_PASSWORD, Arrays.asList(noPermsGroup));
		
		unrestrictedFolder = ContentNodeTestDataUtils.createFolder(rootFolder, "unrestricted");
		restrictedFolder = ContentNodeTestDataUtils.createFolder(rootFolder, "restricted");
		
		int unrestrictedFolderId = unrestrictedFolder.getId();

		PermHandler.setPermissions(
			Folder.TYPE_FOLDER,
			unrestrictedFolderId,
			Arrays.asList(roGroup),
			new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString());

		PermHandler.setPermissions(
			Folder.TYPE_FOLDER,
			unrestrictedFolderId,
			Arrays.asList(rwGroup),
			new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_FOLDER_UPDATE, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE).toString());

		PermHandler.setPermissions(
			Page.TYPE_PAGE,
			unrestrictedFolderId,
			Arrays.asList(roGroup),
			new Permission(PermHandler.PERM_PAGE_VIEW).toString());
	
		PermHandler.setPermissions(
			Page.TYPE_PAGE,
			unrestrictedFolderId,
			Arrays.asList(rwGroup),
			new Permission(PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE).toString());

		t.commit(false);
	}

	public MultiObjectLoadTestBase(TestPermission permission, boolean forUpdate) throws NodeException {
		this.permission = permission;
		this.forUpdate = forUpdate;
		
		String user;

		switch (permission) {
		case ReadOnly:
			user = RO_USER_LOGIN;
			break;

		case ReadWrite:
			user = RW_USER_LOGIN;
			break;

		case None:
		default:
			user = NO_PERMS_USER_LOGIN;
			break;
		}
		
		testContext.getContext().login(user, DEFAULT_PASSWORD);
	}
	
	private void checkResult(List<? extends ContentNodeItem> list, int unrestricted, int restricted) {
		Set<Integer> ids = list.stream().map(item -> item.getId()).collect(Collectors.toSet());
		StringJoiner joined = new StringJoiner(", ", "[", "]");
			
		for (ContentNodeItem item: list) {
			joined.add(item.getId().toString());
		}

		assertFalse(
			"Result must not contain restricted item with id " + restricted + ", but was " + joined,
			ids.contains(restricted));

		if (permission == TestPermission.None) {
			assertTrue(
				"Result should be empty, but was: " + joined.toString(),
				ids.isEmpty());
		} else if (permission == TestPermission.ReadOnly) {
			if (forUpdate) {
				assertFalse(
					"Result must not contain read only item with id " + unrestricted + ", but was " + joined,
					ids.contains(unrestricted));
			} else {
				assertTrue(
					"Result must contain read only item with id " + unrestricted + ", but was " + joined,
					ids.contains(unrestricted));
			}
		} else {
			assertTrue(
				"Result must contain unrestricted item with id " + unrestricted + ", but was " + joined,
				ids.contains(unrestricted));
		}
	}

	@Test
	public void testLoadFiles() throws Exception {
		int unrestrictedFileId = ContentNodeTestDataUtils.createFile(unrestrictedFolder, "unrestrictedFile", new byte[0]).getId();
		int restrictedFileId = ContentNodeTestDataUtils.createFile(restrictedFolder, "restrictedFile", new byte[0]).getId();
		MultiObjectLoadRequest request = new MultiObjectLoadRequest();
		
		request.setIds(Arrays.asList(unrestrictedFileId, restrictedFileId, NON_EXISTENT));
		request.setForUpdate(forUpdate);
		
		MultiFileLoadResponse response = ContentNodeRESTUtils.getFileResource().load(request);
		List<File> files = response.getFiles();

		ContentNodeRESTUtils.assertResponseOK(response);
		checkResult(files, unrestrictedFileId, restrictedFileId);
	}
	
	@Test
	public void testLoadImages() throws Exception {
		byte[] image = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));
		int unrestrictedImageId = ContentNodeTestDataUtils.createImage(
			unrestrictedFolder,
			"unrestrictedImage",
			image,
			null).getId();
		int restrictedImageId = ContentNodeTestDataUtils.createImage(
			restrictedFolder,
			"restrictedImage",
			image,
			null).getId();
		MultiObjectLoadRequest request = new MultiObjectLoadRequest();
		
		request.setIds(Arrays.asList(unrestrictedImageId, restrictedImageId, NON_EXISTENT));
		request.setForUpdate(forUpdate);
		
		MultiImageLoadResponse response = ContentNodeRESTUtils.getImageResource().load(request);
		List<Image> files = response.getImages();

		ContentNodeRESTUtils.assertResponseOK(response);
		checkResult(files, unrestrictedImageId, restrictedImageId);
	}
	
	@Test
	public void testLoadFolders() throws Exception {
		MultiFolderLoadRequest request = new MultiFolderLoadRequest();
		int unrestrictedFolderId = unrestrictedFolder.getId();
		int restrictedFolderId = restrictedFolder.getId();
		
		request.setIds(Arrays.asList(unrestrictedFolderId, restrictedFolderId, NON_EXISTENT));
		request.setForUpdate(forUpdate);
		
		MultiFolderLoadResponse response = ContentNodeRESTUtils.getFolderResource().load(request);
		List<com.gentics.contentnode.rest.model.Folder> files = response.getFolders();

		ContentNodeRESTUtils.assertResponseOK(response);
		checkResult(files, unrestrictedFolderId, restrictedFolderId);
	}
	
	@Test
	public void testLoadPages() throws Exception {
		MultiPageLoadRequest request = new MultiPageLoadRequest();
		int unrestrictedPageId = ContentNodeTestDataUtils.createTemplateAndPage(
			unrestrictedFolder,
			"unrestrictedPage").getId();
		int restrictedPageId = ContentNodeTestDataUtils.createTemplateAndPage(
			restrictedFolder,
			"restrictedPage").getId();

		request.setIds(Arrays.asList(unrestrictedPageId, restrictedPageId, 0));
		request.setForUpdate(forUpdate);
			
		MultiPageLoadResponse response = ContentNodeRESTUtils.getPageResource().load(request);
		List<com.gentics.contentnode.rest.model.Page> files = response.getPages();

		ContentNodeRESTUtils.assertResponseOK(response);
		checkResult(files, unrestrictedPageId, restrictedPageId);
	}
}
