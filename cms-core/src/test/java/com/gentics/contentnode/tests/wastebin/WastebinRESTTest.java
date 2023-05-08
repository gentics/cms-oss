package com.gentics.contentnode.tests.wastebin;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.request.WastebinSearch;
import com.gentics.contentnode.rest.model.response.FileListResponse;
import com.gentics.contentnode.rest.model.response.FolderListResponse;
import com.gentics.contentnode.rest.model.response.FolderObjectCountResponse;
import com.gentics.contentnode.rest.model.response.ImageListResponse;
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
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test cases for getting objects in the wastebin via the REST API
 */
@RunWith(value = Parameterized.class)
public class WastebinRESTTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Tested node
	 */
	private static Node node;

	private static Folder folder1;

	private static Folder folder11;

	private static Folder folder12;

	private static Folder folder2;

	private static Folder folder21;

	private static Folder folder22;

	private static Page page1;

	private static Page page2;

	private static Page page3;

	private static Page page4;

	private static File file1;

	private static File file2;

	private static File file3;

	private static File file4;

	private static File image1;

	private static File image2;

	private static File image3;

	private static File image4;

	private boolean wastebin;

	private boolean wastebinPermission;

	private boolean useLegacyEndpoint;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: wastebin {0}, permission {1}, legacy endpoint {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean wastebin : Arrays.asList(true, false)) {
			for (boolean wastebinPermission : Arrays.asList(true, false)) {
				for (boolean useLegacyEndpoint : Arrays.asList(true, false)) {
					data.add(new Object[] { wastebin, wastebinPermission, useLegacyEndpoint });
				}
			}
		}
		return data;
	}

	/**
	 * Create test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.WASTEBIN.toString().toLowerCase(), true);
		node = ContentNodeTestDataUtils.createNode("node", "Node", PublishTarget.NONE);
		t.commit(false);

		// create two groups with users
		UserGroup nodeGroup = t.getObject(UserGroup.class, 2);
		UserGroup groupWithPermission = Creator.createUsergroup("With Wastebin Permission", "", nodeGroup);
		Creator.createUser("perm", "perm", "perm", "perm", "", Arrays.asList(groupWithPermission));
		UserGroup groupWithoutPermission = Creator.createUsergroup("Without Wastebin Permission", "", nodeGroup);
		Creator.createUser("noperm", "noperm", "noperm", "noperm", "", Arrays.asList(groupWithoutPermission));

		// set permissions
		PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(groupWithPermission), new Permission(PermHandler.PERM_VIEW,
				PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_NODE_WASTEBIN).toString());
		PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(groupWithoutPermission), new Permission(PermHandler.PERM_VIEW,
				PermHandler.PERM_PAGE_VIEW).toString());
		t.commit(false);

		// create folder structures
		folder1 = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder 1");
		folder11 = ContentNodeTestDataUtils.createFolder(folder1, "Folder 1.1");
		folder12 = ContentNodeTestDataUtils.createFolder(folder1, "Folder 1.2");
		folder2 = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder 2");
		folder21 = ContentNodeTestDataUtils.createFolder(folder2, "Folder 2.1");
		folder22 = ContentNodeTestDataUtils.createFolder(folder2, "Folder 2.2");
		t.commit(false);

		// create pages
		Template template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Source", "Template");
		page1 = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page 1");
		page2 = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Page 2");
		page3 = ContentNodeTestDataUtils.createPage(folder1, template, "Page 3");
		page4 = ContentNodeTestDataUtils.createPage(folder2, template, "Page 4");
		t.commit(false);

		// create files
		file1 = ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile1.txt", "Contents".getBytes());
		file2 = ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile2.txt", "Contents".getBytes());
		file3 = ContentNodeTestDataUtils.createFile(folder1, "testfile3.txt", "Contents".getBytes());
		file4 = ContentNodeTestDataUtils.createFile(folder2, "testfile4.txt", "Contents".getBytes());
		t.commit(false);

		// create images
		image1 = ContentNodeTestDataUtils.createFile(node.getFolder(), "blume1.jpg", GenericTestUtils.getPictureResource("blume.jpg"), null);
		image2 = ContentNodeTestDataUtils.createFile(node.getFolder(), "blume2.jpg", GenericTestUtils.getPictureResource("blume.jpg"), null);
		image3 = ContentNodeTestDataUtils.createFile(folder1, "blume3.jpg", GenericTestUtils.getPictureResource("blume.jpg"), null);
		image4 = ContentNodeTestDataUtils.createFile(folder2, "blume4.jpg", GenericTestUtils.getPictureResource("blume.jpg"), null);
		t.commit(false);

		// delete data
		folder1.delete();
		page1.delete();
		file1.delete();
		image1.delete();
		t.commit(false);

		// reload objects, so that they get their deleted status set
		try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
			folder1 = t.getObject(folder1);
			folder11 = t.getObject(folder11);
			folder12 = t.getObject(folder12);
			page1 = t.getObject(page1);
			page3 = t.getObject(page3);
			file1 = t.getObject(file1);
			file3 = t.getObject(file3);
			image1 = t.getObject(image1);
			image3 = t.getObject(image3);
		}
	}

	/**
	 * Create a test instance
	 * @param wastebin true to fetch objects from wastebin, false if not
	 * @param wastebinPermission true to test with wastebin permission, false to test without
	 */
	public WastebinRESTTest(boolean wastebin, boolean wastebinPermission, boolean useLegacyEndpoint) {
		this.wastebin = wastebin;
		this.wastebinPermission = wastebinPermission;
		this.useLegacyEndpoint = useLegacyEndpoint;
	}

	@Before
	public void setup() throws Exception {
		if (wastebinPermission) {
			testContext.getContext().login("perm", "perm");
		} else {
			testContext.getContext().login("noperm", "noperm");
		}
	}

	/**
	 * Test listing pages with/without wastebin status
	 * @throws Exception
	 */
	@Test
	public void testListPages() throws Exception {
		List<? extends ContentNodeItem> pages = getPages(false, wastebin, useLegacyEndpoint);
		assertContainsObject(pages, page1, wastebin && wastebinPermission);
		assertContainsObject(pages, page2, true);
		assertContainsObject(pages, page3, false);
		assertContainsObject(pages, page4, false);

		pages = getPages(true, wastebin, useLegacyEndpoint);
		assertContainsObject(pages, page1, wastebin && wastebinPermission);
		assertContainsObject(pages, page2, true);
		assertContainsObject(pages, page3, wastebin && wastebinPermission);
		assertContainsObject(pages, page4, true);
	}

	/**
	 * Test listing folders with/without wastebin status
	 * @throws Exception
	 */
	@Test
	public void testListFolders() throws Exception {
		List<? extends ContentNodeItem> folders = getFolders(false, wastebin, false, useLegacyEndpoint);
		assertContainsObject(folders, folder1, wastebin && wastebinPermission);
		assertContainsObject(folders, folder11, false);
		assertContainsObject(folders, folder12, false);
		assertContainsObject(folders, folder2, true);
		assertContainsObject(folders, folder21, false);
		assertContainsObject(folders, folder22, false);

		folders = getFolders(true, wastebin, false, useLegacyEndpoint);
		assertContainsObject(folders, folder1, wastebin && wastebinPermission);
		assertContainsObject(folders, folder11, wastebin && wastebinPermission);
		assertContainsObject(folders, folder12, wastebin && wastebinPermission);
		assertContainsObject(folders, folder2, true);
		assertContainsObject(folders, folder21, true);
		assertContainsObject(folders, folder22, true);
	}

	/**
	 * Test fetching folder tree
	 * @throws Exception
	 */
	@Test
	public void testFolderTree() throws Exception {
		List<? extends ContentNodeItem> folders = getFolders(true, wastebin, true, useLegacyEndpoint);
		assertContainsObject(folders, folder1, wastebin && wastebinPermission);
		assertContainsObject(folders, folder11, wastebin && wastebinPermission);
		assertContainsObject(folders, folder12, wastebin && wastebinPermission);
		assertContainsObject(folders, folder2, true);
		assertContainsObject(folders, folder21, true);
		assertContainsObject(folders, folder22, true);
	}

	/**
	 * Test listing files with/without wastebin status
	 * @throws Exception
	 */
	@Test
	public void testListFiles() throws Exception {
		List<? extends ContentNodeItem> files = getFiles(false, wastebin, useLegacyEndpoint);
		assertContainsObject(files, file1, wastebin && wastebinPermission);
		assertContainsObject(files, file2, true);
		assertContainsObject(files, file3, false);
		assertContainsObject(files, file4, false);

		files = getFiles(true, wastebin, useLegacyEndpoint);
		assertContainsObject(files, file1, wastebin && wastebinPermission);
		assertContainsObject(files, file2, true);
		assertContainsObject(files, file3, wastebin && wastebinPermission);
		assertContainsObject(files, file4, true);
	}

	/**
	 * Test listing images with/without wastebin status
	 * @throws Exception
	 */
	@Test
	public void testListImages() throws Exception {
		List<? extends ContentNodeItem> images = getImages(false, wastebin, useLegacyEndpoint);
		assertContainsObject(images, image1, wastebin && wastebinPermission);
		assertContainsObject(images, image2, true);
		assertContainsObject(images, image3, false);
		assertContainsObject(images, image4, false);

		images = getImages(true, wastebin, useLegacyEndpoint);
		assertContainsObject(images, image1, wastebin && wastebinPermission);
		assertContainsObject(images, image2, true);
		assertContainsObject(images, image3, wastebin && wastebinPermission);
		assertContainsObject(images, image4, true);
	}

	/**
	 * Test counting folders
	 * @throws Exception
	 */
	@Test
	public void testCountFolders() throws Exception {
		// Makes no difference for this test.
		if (useLegacyEndpoint) {
			return;
		}

		assertEquals("Check # of folders", wastebin && wastebinPermission ? 2 : 1, getObjectCounts().getFolders());
	}

	/**
	 * Test counting pages
	 * @throws Exception
	 */
	@Test
	public void testCountPages() throws Exception {
		// Makes no difference for this test.
		if (useLegacyEndpoint) {
			return;
		}

		assertEquals("Check # of pages", wastebin && wastebinPermission ? 2 : 1, getObjectCounts().getPages());
	}

	/**
	 * Test counting files
	 * @throws Exception
	 */
	@Test
	public void testCountFiles() throws Exception {
		// Makes no difference for this test.
		if (useLegacyEndpoint) {
			return;
		}

		assertEquals("Check # of files", wastebin && wastebinPermission ? 2 : 1, getObjectCounts().getFiles());
	}

	/**
	 * Test counting images
	 * @throws Exception
	 */
	@Test
	public void testCountImages() throws Exception {
		// Makes no difference for this test.
		if (useLegacyEndpoint) {
			return;
		}

		assertEquals("Check # of images", wastebin && wastebinPermission ? 2 : 1, getObjectCounts().getImages());
	}

	/**
	 * Get pages
	 * @param recursive true to get pages recursively
	 * @param wastebin true to get pages from wastebin
	 * @param useLegacyEndpoint true to use folder resource getPages() instead of page resource list()
	 * @return list of pages
	 * @throws Exception
	 */
	protected List<? extends ContentNodeItem> getPages(boolean recursive, boolean wastebin, boolean useLegacyEndpoint) throws Exception {
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(node.getFolder().getId().toString()).setRecursive(recursive);
		WastebinParameterBean wastebinParams = new WastebinParameterBean().setWastebinSearch(wastebin ? WastebinSearch.include : WastebinSearch.exclude);
		PageListParameterBean pageListParams = new PageListParameterBean();

		if (useLegacyEndpoint) {
			LegacyPageListResponse legacyResponse = ContentNodeRESTUtils.getFolderResource().getPages(
				inFolder.folderId,
				inFolder,
				pageListParams,
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean(),
				new LegacyPagingParameterBean(),
				new PublishableParameterBean(),
				wastebinParams);

			ContentNodeRESTUtils.assertResponseOK(legacyResponse);

			return legacyResponse.getPages();
		}

		PageListResponse response = ContentNodeRESTUtils.getPageResource().list(
			inFolder,
			pageListParams,
			new FilterParameterBean(),
			new SortParameterBean(),
			new PagingParameterBean(),
			new PublishableParameterBean(),
			wastebinParams);

		ContentNodeRESTUtils.assertResponseOK(response);
		return response.getItems();
	}

	/**
	 * Get folders
	 * @param recursive true to get recursively
	 * @param wastebin true to get from wastebin
	 * @param tree true to get as tree
	 * @param useLegacyEndpoint true to use folder resource getPages() instead of page resource list()
	 * @return list of folders
	 * @throws Exception
	 */
	protected List<? extends ContentNodeItem> getFolders(boolean recursive, boolean wastebin, boolean tree, boolean useLegacyEndpoint) throws Exception {
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(node.getFolder().getId().toString()).setRecursive(recursive);
		FolderListParameterBean folderListParams = new FolderListParameterBean().setTree(tree);
		LegacySortParameterBean sort = new LegacySortParameterBean().setSortBy("asc");
		WastebinParameterBean wastebinParams = new WastebinParameterBean().setWastebinSearch(wastebin ? WastebinSearch.include : WastebinSearch.exclude);
		List<com.gentics.contentnode.rest.model.Folder> responseFolders;

		if (useLegacyEndpoint) {
			LegacyFolderListResponse response = ContentNodeRESTUtils.getFolderResource().getFolders(
				inFolder.folderId,
				folderListParams.recursiveIds,
				folderListParams.addPrivileges,
				inFolder,
				folderListParams,
				new LegacyFilterParameterBean(),
				sort,
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				wastebinParams);

			ContentNodeRESTUtils.assertResponseOK(response);

			responseFolders = response.getFolders();
		} else {
			FolderListResponse response = ContentNodeRESTUtils.getFolderResource().list(
				inFolder,
				folderListParams,
				new FilterParameterBean(),
				sort.toSortParameterBean(),
				new PagingParameterBean(),
				new EditableParameterBean(),
				wastebinParams);

			ContentNodeRESTUtils.assertResponseOK(response);

			responseFolders = response.getItems();
		}

		if (tree) {
			List<com.gentics.contentnode.rest.model.Folder> items = new ArrayList<>();
			List<com.gentics.contentnode.rest.model.Folder> folders = new ArrayList<>(responseFolders);
			while (!folders.isEmpty()) {
				com.gentics.contentnode.rest.model.Folder folder = folders.remove(0);
				items.add(folder);
				folders.addAll(folder.getSubfolders());
			}
			return items;
		} else {
			return responseFolders;
		}
	}

	/**
	 * Get files
	 * @param recursive true to get recursively
	 * @param wastebin true to get from wastebin
	 * @param useLegacyEndpoint true to use folder resource getFiles() instead of file resource list()
	 * @return list of files
	 * @throws Exception
	 */
	protected List<? extends ContentNodeItem> getFiles(boolean recursive, boolean wastebin, boolean useLegacyEndpoint) throws Exception {
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(node.getFolder().getId().toString()).setRecursive(recursive);
		WastebinParameterBean wastebinParameterBean = new WastebinParameterBean().setWastebinSearch(wastebin ? WastebinSearch.include : WastebinSearch.exclude);

		if (useLegacyEndpoint) {
			LegacyFileListResponse legacyResponse = ContentNodeRESTUtils.getFolderResource().getFiles(
				inFolder.folderId,
				inFolder,
				new FileListParameterBean(),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean(),
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				wastebinParameterBean);

				ContentNodeRESTUtils.assertResponseOK(legacyResponse);

				return legacyResponse.getFiles();
		}

		FileListResponse response = ContentNodeRESTUtils.getFileResource().list(
			inFolder,
			new FileListParameterBean(),
			new FilterParameterBean(),
			new SortParameterBean(),
			new PagingParameterBean(),
			new EditableParameterBean(),
			wastebinParameterBean);

		ContentNodeRESTUtils.assertResponseOK(response);

		return response.getItems();
	}

	/**
	 * Get images
	 * @param recursive true to get recursively
	 * @param wastebin true to get from wastebin
	 * @param useLegacyEndpoint true to use folder resource getImages() instead of image resource list()
	 * @return list of files
	 * @throws Exception
	 */
	protected List<? extends ContentNodeItem> getImages(boolean recursive, boolean wastebin, boolean useLegacyEndpoint) throws Exception {
		InFolderParameterBean inFolder = new InFolderParameterBean().setFolderId(node.getFolder().getId().toString()).setRecursive(recursive);
		WastebinParameterBean wastebinParameterBean = new WastebinParameterBean().setWastebinSearch(wastebin ? WastebinSearch.include : WastebinSearch.exclude);

		if (useLegacyEndpoint) {
			LegacyFileListResponse legacyResponse = ContentNodeRESTUtils.getFolderResource().getImages(
				inFolder.folderId,
				inFolder,
				new FileListParameterBean(),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean(),
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				wastebinParameterBean);

			ContentNodeRESTUtils.assertResponseOK(legacyResponse);

			return legacyResponse.getFiles();
		}

		ImageListResponse response = ContentNodeRESTUtils.getImageResource().list(
			inFolder,
			new FileListParameterBean(),
			new FilterParameterBean(),
			new SortParameterBean(),
			new PagingParameterBean(),
			new EditableParameterBean(),
			wastebinParameterBean);

		ContentNodeRESTUtils.assertResponseOK(response);

		return response.getItems();
	}

	/**
	 * Assert containement of the given object in the list of items (coming from REST). If the object is found, it is also checked, whether the "deleted" status is set accordingly
	 * @param items list of items
	 * @param object asserted object
	 * @param expected true if the object is expected to be contained, false if not
	 * @throws Exception
	 */
	protected void assertContainsObject(List<? extends ContentNodeItem> items, NodeObject object, boolean expected) throws Exception {
		boolean found = false;
		for (ContentNodeItem item : items) {
			if (item.getId().equals(object.getId())) {
				if (expected) {
					if (object.isDeleted()) {
						assertNotNull("REST Model of " + object + " must contain the delete information", item.getDeleted());
						assertThat(item.getDeleted().getAt()).isGreaterThan(0);
					} else {
						assertNotNull("REST Model of " + object + " must contain the delete information", item.getDeleted());
						assertThat(item.getDeleted().getAt()).isEqualTo(0);
					}
					found = true;
					break;
				} else {
					fail("Unepxectedly found " + object + " in response");
				}
			}
		}

		if (expected && !found) {
			fail("Did not find expected " + object + " in response");
		}
	}

	/**
	 * Get the object counts
	 * @return object counts
	 * @throws Exception
	 */
	protected FolderObjectCountResponse getObjectCounts() throws Exception {
		FolderResource res = ContentNodeRESTUtils.getFolderResource();
		Integer folderId = node.getFolder().getId();

		FolderObjectCountResponse response = res.getObjectCounts(
			folderId,
			null,
			null,
			false,
			new InFolderParameterBean().setFolderId(String.valueOf(folderId)),
			new WastebinParameterBean().setWastebinSearch(wastebin ? WastebinSearch.include : WastebinSearch.exclude));
		ContentNodeRESTUtils.assertResponseOK(response);
		return response;
	}
}
