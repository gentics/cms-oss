package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getImageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateAndPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
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
import com.gentics.contentnode.tests.utils.Auth;
import com.gentics.contentnode.tests.utils.Auth.AuthType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

/**
 * Tests for loading multiple objects via the REST API.
 */
@RunWith(Parameterized.class)
public class MultiObjectLoadTest {

	/**
	 * Permissions to be tested against.
	 */
	public enum TestPermission {
		None,
		ReadOnly,
		ReadWrite
	}

	private final static String NO_PERMS_USER_LOGIN = "nopermsuser";
	private final static String RO_USER_LOGIN = "rouser";
	private final static String RW_USER_LOGIN = "rwuser";
	private final static String DEFAULT_PASSWORD = "password";

	private final static int NON_EXISTENT = 0;

	protected static SystemUser noPermsUser;

	protected static Auth noPermsUserAuth;

	protected static SystemUser roUser;

	protected static Auth roUserAuth;

	protected static SystemUser rwUser;

	protected static Auth rwUserAuth;

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Folder unrestrictedFolder;
	private static Folder restrictedFolder;

	@Parameter(0)
	public TestPermission permission;

	@Parameter(1)
	public boolean forUpdate;

	@Parameter(2)
	public boolean fillWithNulls;

	@Parameter(3)
	public AuthType authType;

	/**
	 * Current authentication
	 */
	protected Auth authentication;

	@Parameters(name = "{index}: permission {0}, forUpdate {1}, fillWithNulls {2}, auth {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (TestPermission perm: TestPermission.values()) {
			for (Boolean forUpdate : List.of(true, false)) {
				for (Boolean fillWithNulls : List.of(true, false)) {
					for (AuthType authType : List.of(AuthType.LOGIN, AuthType.TOKEN)) {
						data.add(new Object[] { perm, forUpdate, fillWithNulls, authType });
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setUpOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		Node node = supply(() -> createNode());
		Folder rootFolder = supply(() -> node.getFolder());

		UserGroup rwGroup = supply(t -> t.getObject(UserGroup.class, NODE_GROUP_ID));
		UserGroup roGroup = supply(() -> createUserGroup(
			"Read only group", NODE_GROUP_ID));
		UserGroup noPermsGroup = supply(() -> createUserGroup(
			"No permissions group", roGroup.getId()));

		operate(() -> {
			rwUser = createSystemUser(
					"Read", "Write", "", RW_USER_LOGIN, DEFAULT_PASSWORD, Arrays.asList(rwGroup));
			roUser = createSystemUser(
					"Read", "Only", "", RO_USER_LOGIN, DEFAULT_PASSWORD, Arrays.asList(roGroup));
			noPermsUser = createSystemUser(
					"No", "Permissions", "", NO_PERMS_USER_LOGIN, DEFAULT_PASSWORD, Arrays.asList(noPermsGroup));
		});
		rwUserAuth = new Auth(rwUser);
		roUserAuth = new Auth(roUser);
		noPermsUserAuth = new Auth(noPermsUser);

		unrestrictedFolder = supply(() -> createFolder(rootFolder, "unrestricted"));
		restrictedFolder = supply(() -> createFolder(rootFolder, "restricted"));

		int unrestrictedFolderId = unrestrictedFolder.getId();

		operate(() -> {
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
		});
	}

	@Before
	public void setup() throws NodeException {
		switch (permission) {
		case ReadOnly:
			authentication = roUserAuth;
			break;

		case ReadWrite:
			authentication = rwUserAuth;
			break;

		case None:
		default:
			authentication = noPermsUserAuth;
			break;
		}
	}

	private void checkResult(List<? extends ContentNodeItem> list, int unrestricted, int restricted) {
		List<Integer> ids = list.stream().map(item -> item != null ? item.getId() : null).collect(Collectors.toList());

		List<Integer> expected = new ArrayList<>(3);

		// unrestricted ID depends on permission
		switch(permission) {
		case None:
			expected.add(0, null);
			break;
		case ReadOnly:
			expected.add(0, forUpdate ? null : unrestricted);
			break;
		case ReadWrite:
			expected.add(0, unrestricted);
			break;
		}

		expected.add(1, null);
		expected.add(2, null);

		if (!fillWithNulls) {
			expected.removeIf(entry -> entry == null);
		}

		assertThat(ids).as("Returned item IDs").containsExactlyElementsOf(expected);
	}

	@Test
	public void testLoadFiles() throws Exception {
		int unrestrictedFileId = supply(() -> createFile(unrestrictedFolder, "unrestrictedFile", "Unrestricted File Contents".getBytes()).getId());
		int restrictedFileId = supply(() -> createFile(restrictedFolder, "restrictedFile", "Restricted File Contents".getBytes()).getId());

		authentication.withAuth(authType, () -> {
			MultiObjectLoadRequest request = new MultiObjectLoadRequest();

			request.setIds(Arrays.asList(unrestrictedFileId, restrictedFileId, NON_EXISTENT));
			request.setForUpdate(forUpdate);

			MultiFileLoadResponse response = getFileResource().load(request, fillWithNulls);
			List<File> files = response.getFiles();

			assertResponseOK(response);
			checkResult(files, unrestrictedFileId, restrictedFileId);
		});
	}

	@Test
	public void testLoadImages() throws Exception {
		byte[] image = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));
		int unrestrictedImageId = supply(() -> createImage(
			unrestrictedFolder,
			"unrestrictedImage",
			image,
			null).getId());
		int restrictedImageId = supply(() -> createImage(
			restrictedFolder,
			"restrictedImage",
			image,
			null).getId());

		authentication.withAuth(authType, () -> {
			MultiObjectLoadRequest request = new MultiObjectLoadRequest();

			request.setIds(Arrays.asList(unrestrictedImageId, restrictedImageId, NON_EXISTENT));
			request.setForUpdate(forUpdate);

			MultiImageLoadResponse response = getImageResource().load(request, fillWithNulls);
			List<Image> files = response.getImages();

			assertResponseOK(response);
			checkResult(files, unrestrictedImageId, restrictedImageId);
		});
	}

	@Test
	public void testLoadFolders() throws Exception {
		authentication.withAuth(authType, () -> {
			MultiFolderLoadRequest request = new MultiFolderLoadRequest();
			int unrestrictedFolderId = unrestrictedFolder.getId();
			int restrictedFolderId = restrictedFolder.getId();

			request.setIds(Arrays.asList(unrestrictedFolderId, restrictedFolderId, NON_EXISTENT));
			request.setForUpdate(forUpdate);

			MultiFolderLoadResponse response = getFolderResource().load(request, fillWithNulls);
			List<com.gentics.contentnode.rest.model.Folder> files = response.getFolders();

			assertResponseOK(response);
			checkResult(files, unrestrictedFolderId, restrictedFolderId);
		});
	}

	@Test
	public void testLoadPages() throws Exception {
		Page unrestrictedPage = supply(() -> createTemplateAndPage(
			unrestrictedFolder,
			"unrestrictedPage"));
		consume(Page::unlock, unrestrictedPage);

		Page restrictedPage = supply(() -> createTemplateAndPage(
			restrictedFolder,
			"restrictedPage"));
		consume(Page::unlock, restrictedPage);

		authentication.withAuth(authType, () -> {
			MultiPageLoadRequest request = new MultiPageLoadRequest();
			request.setIds(Arrays.asList(unrestrictedPage.getId(), restrictedPage.getId(), 0));
			request.setForUpdate(forUpdate);

			MultiPageLoadResponse response = getPageResource().load(request, fillWithNulls);
			List<com.gentics.contentnode.rest.model.Page> files = response.getPages();

			assertResponseOK(response);
			checkResult(files, unrestrictedPage.getId(), restrictedPage.getId());
		});
	}
}
