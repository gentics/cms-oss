package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.rest.util.MiscUtils.doSetPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.Privilege;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderListRequest;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.GroupsPermBitsResponse;
import com.gentics.contentnode.rest.model.response.LegacyFolderListResponse;
import com.gentics.contentnode.rest.model.response.PermBitsResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.PermResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Sandbox Test for rendering a page
 */
public class PermissionSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * ID of the root folder
	 */
	public final static int ROOT_FOLDER_ID = 27;

	/**
	 * ID of the permission group
	 */
	public final static int GROUP_ID = 11;

	/**
	 * The number of workers to spawn in multithreading tests
	 */
	public final static int NUM_THREADWORKERS = 64;

	/**
	 * Nesting level for folders
	 */
	public final static int FOLDER_LEVELS = 5;

	/**
	 * Number of folders per level
	 */
	public final static int FOLDERS_PER_LEVEL = 10;

	/**
	 * Thread safe GenericResponse object
	 */
	final AtomicReference<GenericResponse> atomicGenericResponse = new AtomicReference<GenericResponse>();

	@Before
	public void setUp() throws Exception {
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();

		prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		// In tests, background jobs should always block
		prefs.setProperty("backgroundjob_foreground_time", "999999");
		testContext.getContext().getTransaction().commit();
	}

	/**
	 * Test listing permissions of different folders
	 * @throws Exception
	 */
	@Test
	public void testListFolderPermissions() throws Exception {
		int subFolderId = supply(() -> {
			// Create a folder next to the page
			FolderCreateRequest folderCreateRequest = new FolderCreateRequest();

			folderCreateRequest.setMotherId(String.valueOf(ROOT_FOLDER_ID));
			folderCreateRequest.setName("some none important folder");
			FolderLoadResponse folderLoadResponse = getFolderResource().create(folderCreateRequest);

			return folderLoadResponse.getFolder().getId();
		});

		// Setup our test data
		Map<Integer, String> folderIdsAndPermissionBits = new HashMap<Integer, String>();
		// Root folder
		folderIdsAndPermissionBits.put(ROOT_FOLDER_ID, "11111110111111111111011100000100");
		// Sub folder
		folderIdsAndPermissionBits.put(subFolderId,    "11111110111111111111111100000100");

		for (Map.Entry<Integer, String> folderIdAndPermissionBit : folderIdsAndPermissionBits.entrySet()) {
			GroupsPermBitsResponse renderResponse = new PermResourceImpl().list(TypePerms.folder.name(), folderIdAndPermissionBit.getKey());
			assertResponseOK(renderResponse);

			Map<Integer, String> groups = renderResponse.getGroups();

			assertThat(groups).as("Group IDs").containsKey(GROUP_ID);
			assertThat(groups.get(GROUP_ID)).as("Permission bits for group " + GROUP_ID).isEqualTo(folderIdAndPermissionBit.getValue());
		}
	}

	/**
	 * Test listing permissions of different folders by type ID
	 * @throws Exception
	 */
	@Test
	public void testListFolderPermissionsByTypeId() throws Exception {
		int subFolderId = supply(() -> {
			// Create a folder next to the page
			FolderCreateRequest folderCreateRequest = new FolderCreateRequest();

			folderCreateRequest.setMotherId(String.valueOf(ROOT_FOLDER_ID));
			folderCreateRequest.setName("some none important folder");
			FolderLoadResponse folderLoadResponse = getFolderResource().create(folderCreateRequest);

			return folderLoadResponse.getFolder().getId();
		});

		// Setup our test data
		Map<Integer, String> folderIdsAndPermissionBits = new HashMap<Integer, String>();
		// Root folder
		folderIdsAndPermissionBits.put(ROOT_FOLDER_ID, "11111110111111111111011100000100");
		// Sub folder
		folderIdsAndPermissionBits.put(subFolderId,    "11111110111111111111111100000100");

		for (Map.Entry<Integer, String> folderIdAndPermissionBit : folderIdsAndPermissionBits.entrySet()) {
			GroupsPermBitsResponse renderResponse = new PermResourceImpl().list("10002", folderIdAndPermissionBit.getKey());
			assertResponseOK(renderResponse);

			Map<Integer, String> groups = renderResponse.getGroups();

			assertThat(groups).as("Group IDs").containsKey(GROUP_ID);
			assertThat(groups.get(GROUP_ID)).as("Permission bits for group " + GROUP_ID).isEqualTo(folderIdAndPermissionBit.getValue());
		}
	}

	/**
	 * Test getting the privilege map in getFolders request
	 * @throws NodeException
	 */
	@Test
	public void testPrivilegeMapForFolders() throws NodeException {
		// create a folder
		Trx.operate(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(String.valueOf(ROOT_FOLDER_ID));
			request.setName("Folder Name");

			ContentNodeRESTUtils.assertResponseOK(getFolderResource().create(request));
		});

		// get folders without privilege map
		Trx.operate(() -> {
			FolderListRequest request = new FolderListRequest();
			request.setId(String.valueOf(ROOT_FOLDER_ID));
			request.setPrivilegeMap(false);

			LegacyFolderListResponse folderListResponse = getFolderResource().getFolders(request);
			ContentNodeRESTUtils.assertResponseOK(folderListResponse);
			assertThat(folderListResponse.getFolders()).isNotEmpty();
			folderListResponse.getFolders().forEach(f -> {
				assertThat(f.getPrivilegeMap()).isNull();
			});
		});

		// get folders with privilege map
		Trx.operate(() -> {
			FolderListRequest request = new FolderListRequest();
			request.setId(String.valueOf(ROOT_FOLDER_ID));
			request.setPrivilegeMap(true);

			LegacyFolderListResponse folderListResponse = getFolderResource().getFolders(request);
			ContentNodeRESTUtils.assertResponseOK(folderListResponse);

			Set<Privilege> expected = new HashSet<>();
			expected.addAll(Privilege.getAvailable(Folder.TYPE_FOLDER));
			expected.addAll(Privilege.getAvailable(Node.TYPE_NODE));

			assertThat(folderListResponse.getFolders()).isNotEmpty();
			folderListResponse.getFolders().forEach(f -> {
				assertThat(f.getPrivilegeMap()).isNotNull();
				expected.stream().filter(p -> p.getPermBit() >= 0).forEach(p -> {
					assertThat(f.getPrivilegeMap().getPrivileges()).isNotNull().containsKey(p);
				});

				assertThat(f.getPrivilegeMap().getLanguages()).isNotNull().isNotEmpty();
				f.getPrivilegeMap().getLanguages().forEach(langPriv -> {
					expected.stream().filter(p -> p.getRoleCheckType() > 0).forEach(p -> {
						assertThat(langPriv.getPrivileges()).isNotNull().containsKey(p);
					});
				});
			});
		});
	}

	/**
	 * Test getting privilege map from permresource
	 * @throws NodeException
	 */
	@Test
	public void testPrivilegeMap() throws NodeException {
		// create a folder
		Integer folderId = Trx.supply(() -> {
			FolderCreateRequest request = new FolderCreateRequest();
			request.setMotherId(String.valueOf(ROOT_FOLDER_ID));
			request.setName("Folder Name");

			FolderLoadResponse response = getFolderResource().create(request);
			ContentNodeRESTUtils.assertResponseOK(response);

			return response.getFolder().getId();
		});

		// get perms without privilege map
		Trx.operate(() -> {
			PermResourceImpl permResource = new PermResourceImpl();
			PermBitsResponse permBitsResponse = permResource.getPermissions(TypePerms.folder.name(), folderId, -1, -1, -1, false);
			ContentNodeRESTUtils.assertResponseOK(permBitsResponse);
			assertThat(permBitsResponse.getPrivilegeMap()).isNull();
		});

		// get perms with privilege map
		Trx.operate(() -> {
			PermResourceImpl permResource = new PermResourceImpl();
			PermBitsResponse permBitsResponse = permResource.getPermissions(TypePerms.folder.name(), folderId, -1, -1, -1, true);
			ContentNodeRESTUtils.assertResponseOK(permBitsResponse);
			assertThat(permBitsResponse.getPrivilegeMap()).isNotNull();

			Set<Privilege> expected = new HashSet<>();
			expected.addAll(Privilege.getAvailable(Folder.TYPE_FOLDER));
			expected.addAll(Privilege.getAvailable(Node.TYPE_NODE));

			expected.stream().filter(p -> p.getPermBit() >= 0).forEach(p -> {
				assertThat(permBitsResponse.getPrivilegeMap().getPrivileges()).isNotNull().containsKey(p);
			});

			assertThat(permBitsResponse.getPrivilegeMap().getLanguages()).isNotNull().isNotEmpty();
			permBitsResponse.getPrivilegeMap().getLanguages().forEach(langPriv -> {
				expected.stream().filter(p -> p.getRoleCheckType() > 0).forEach(p -> {
					assertThat(langPriv.getPrivileges()).isNotNull().containsKey(p);
				});
			});
		});
	}

	/**
	 * Test whether setting permissions works when done parallely
	 * @throws Exception
	 */
	@Test
	public void testMultiThreadedPermissionSetting() throws Exception {
		UserGroup parentGroup = supply(t -> t.getObject(UserGroup.class, 2));
		UserGroup testGroup = supply(() -> Creator.createUsergroup("testgroup", "", parentGroup));

		ContentLanguage klingon = supply(() -> Creator.createLanguage("Klingon", "tlh"));
		Map<ContentLanguage, String> languagePermissions = new HashMap<ContentLanguage, String>();
		languagePermissions.put(klingon, new Permission(PermHandler.ROLE_VIEW).toString());
		int roleId = supply(() -> Creator.createRole("Testrole", languagePermissions, null, null));

		List<com.gentics.contentnode.rest.model.Folder> folders =
				new ArrayList<com.gentics.contentnode.rest.model.Folder>();

		for (int i = 0; i < NUM_THREADWORKERS; i++) {
			// Create some folders
			FolderCreateRequest folderCreateRequest = new FolderCreateRequest();

			folderCreateRequest.setMotherId(String.valueOf(ROOT_FOLDER_ID));
			folderCreateRequest.setName("Folder " + Integer.toString(i));
			FolderLoadResponse folderLoadResponse = supply(() -> getFolderResource().create(folderCreateRequest));

			folders.add(folderLoadResponse.getFolder());
			operate(() -> PermHandler.setRoles(Folder.TYPE_FOLDER, folderLoadResponse.getFolder().getId(), Arrays.asList(testGroup), Collections.singleton(roleId)));
		}

		// Now spawn 8 threads and run perm requests parallely
		ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADWORKERS);

		int numFoldersPerThread = folders.size() / NUM_THREADWORKERS;
		for (int threadNum = 0; threadNum < NUM_THREADWORKERS; threadNum++) {
			List<com.gentics.contentnode.rest.model.Folder> foldersforOneWorker =
					new ArrayList<com.gentics.contentnode.rest.model.Folder>();

			int n = 0;
			int base = threadNum * numFoldersPerThread;
			while (n < numFoldersPerThread) {
				foldersforOneWorker.add(folders.get(base + n));
				n++;
			}

			executorService.execute(new MultiThreadedPermissionSetter(
					testGroup, foldersforOneWorker, testContext, roleId));
		}

		executorService.shutdown();
		// Wait for the threads to finish
		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

		GenericResponse genericResponse = atomicGenericResponse.get();
		if (genericResponse != null && genericResponse.getResponseInfo().getResponseCode() != ResponseCode.OK) {
			assertResponseOK(atomicGenericResponse.get());
		}
	}

	public class MultiThreadedPermissionSetter implements Runnable {

		UserGroup testGroup = null;
		List<com.gentics.contentnode.rest.model.Folder> folders = null;
		DBTestContext testContext = null;
		int roleId;

		public MultiThreadedPermissionSetter(UserGroup testGroup,
				List<com.gentics.contentnode.rest.model.Folder> folders,
				DBTestContext testContext, int roleId) throws Exception {
			this.testGroup    = testGroup;
			this.folders      = folders;
			this.testContext  = testContext;
			this.roleId		  = roleId;
		}

		public void run() {
			try {
				Transaction t = testContext.startSystemUserTransaction();

				SetPermsRequest setPermsRequest = new SetPermsRequest();
				setPermsRequest.setGroupId(ObjectTransformer.getInteger(this.testGroup.getId(), -1));

				for (int i = 0; i < 50; i++) {
					if (i % 2 == 0) {
						setPermsRequest.setRoleIds(Collections.singleton(roleId));
					} else {
						setPermsRequest.setRoleIds(Collections.emptySet());
					}
					for (com.gentics.contentnode.rest.model.Folder folder : this.folders) {
						if (atomicGenericResponse.get() != null) {
							return;
						}

						setPermsRequest.setPerm(new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString());
						GenericResponse genericResponse = doSetPermissions(TypePerms.folder, folder.getId(),
								setPermsRequest);

						// Commit after each permission setting to throw deadlock exceptions immediately
						t.commit(false);

						if (genericResponse.getResponseInfo().getResponseCode() != ResponseCode.OK) {
							atomicGenericResponse.set(genericResponse);
							return;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Test setting folder permissions for a single group
	 * @throws Exception
	 */
	@Test
	public void testSetFolderPermissions() throws Exception {
		doTestSetFolderPermissions(false, false);
	}

	/**
	 * Test setting folder permissions for a folder and subfolders
	 * @throws Exception
	 */
	@Test
	public void testSetFolderPermissionsSubFolder() throws Exception {
		doTestSetFolderPermissions(true, false);
	}

	/**
	 * Test setting folder permissions for subgroups
	 * @throws Exception
	 */
	@Test
	public void testSetFolderPermissionsSubGroups() throws Exception {
		doTestSetFolderPermissions(false, true);
	}

	/**
	 * Test setting folder permissions for subfolders and subgroups
	 * @throws Exception
	 */
	@Test
	public void testSetFolderPermissionsSubFoldersSubGroups() throws Exception {
		doTestSetFolderPermissions(true, true);
	}

	/**
	 * Test whether reloading the permissions on an object works
	 * when setting all permission bits to 0
	 * @throws Exception
	 */
	@Test
	public void testRemoveAllPermissionsToAnObject() throws Exception {
		Transaction t = testContext.startSystemUserTransaction();

		Node node = ContentNodeTestDataUtils.createNode("testnode", "Test Node", PublishTarget.NONE);
		Folder folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");

		UserGroup testGroup = Creator.createUsergroup("Testgroup", "", t.getObject(UserGroup.class, 2));
		t.commit(false);

		// First give the group view permission and check if it was set
		this.setFolderPermissionsWithRestApiAndAssert(folder, testGroup,
				new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW).toString(),
				false, false, ResponseCode.OK);

		// Now remove the permissions again and check if it was removed
		// The group shouldn't have any permissions on that object anymore
		this.setFolderPermissionsWithRestApiAndAssert(folder, testGroup, PermHandler.EMPTY_PERM,
				false, false, ResponseCode.OK);
	}

	/**
	 * Recursively create folder structure
	 * @param mother current mother folder
	 * @param level current level
	 * @throws NodeException
	 */
	protected void recursivelyCreateFolders(Folder mother, int level) throws NodeException {
		if (level >= FOLDER_LEVELS) {
			return;
		}

		for (int i = 1; i <= FOLDERS_PER_LEVEL; i++) {
			String name = mother.getName() + "." + i;
			Folder folder = Creator.createFolder(mother, name, "/");
			recursivelyCreateFolders(folder, level + 1);
		}
	}

	/**
	 * Set permissions (according to the flags) and check whether the permissions were set
	 * @param subFolders true to set for subfolders
	 * @param subGroups true to set for subgroups
	 * @throws Exception
	 */
	protected void doTestSetFolderPermissions(boolean subFolders, boolean subGroups) throws Exception {
		String permissions = Permissions.get(PermHandler.PERM_VIEW).toString();


		Node node = supply(() -> createNode("testnode", "Test Node", PublishTarget.NONE));
		Folder folder = supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));

		UserGroup testGroup = supply(t -> Creator.createUsergroup("Testgroup", "", t.getObject(UserGroup.class, 2)));
		UserGroup subGroup = supply(() -> Creator.createUsergroup("Subgroup", "", testGroup));

		operate(() -> setFolderPermissionsWithRestApiAndAssert(node.getFolder(), testGroup, permissions, subFolders, subGroups, ResponseCode.OK));

		operate(() -> assertFolderPermissions(node.getFolder(), testGroup, permissions));
		operate(() -> assertFolderPermissions(folder, testGroup, subFolders ? permissions : PermHandler.EMPTY_PERM));
		operate(() -> assertFolderPermissions(node.getFolder(), subGroup, subGroups ? permissions : PermHandler.EMPTY_PERM));
		operate(() -> assertFolderPermissions(folder, subGroup, (subFolders && subGroups) ? permissions : PermHandler.EMPTY_PERM));
	}

	/**
	 * Assert the permissions on the given folder for the given group
	 * @param folder folder
	 * @param group group
	 * @param expected expected permissions string
	 * @throws NodeException
	 */
	protected void assertFolderPermissions(Folder folder, UserGroup group, String expected) throws NodeException {
		PermResourceImpl resource = new PermResourceImpl();
		GroupsPermBitsResponse permBitsResponse = resource.list(TypePerms.folder.name(), ObjectTransformer.getInt(folder.getId(), 0));
		ContentNodeRESTUtils.assertResponseOK(permBitsResponse);

		assertTrue("Permission list must contain " + group, permBitsResponse.getGroups().containsKey(group.getId()));
		assertEquals("Check permissions on " + folder + " for " + group, expected, permBitsResponse.getGroups().get(group.getId()));
	}

	/**
	 * Assert that the response code is {@link ResponseCode.OK}
	 * @param response response
	 */
	protected void assertResponseOK(GenericResponse response) {
		assertTrue(response.getResponseInfo().getResponseMessage(), response.getResponseInfo().getResponseCode() == ResponseCode.OK);
	}

	/**
	 * Build an SetPermsRequest, set the permissions on an object
	 * and assert the response code
	 * @param objType       Object Type
	 * @param objectId      Object ID
	 * @param groupId       Group ID
	 * @param permissions   The permission bit string
	 * @param subObjects    Whether to set the permissions on subobjects also
	 * @param subGroups     Whether to set the permissions on subgroups also
	 * @param responseCode  the expected response code
	 * @return Response from the rest method
	 * @throws NodeException
	 */
	protected GenericResponse setFolderPermissionsWithRestApiAndAssert(Folder folder, UserGroup userGroup,
			String permissions, boolean subObjects, boolean subGroups, ResponseCode responseCode) throws NodeException {
		SetPermsRequest request = new SetPermsRequest();
		request.setGroupId(ObjectTransformer.getInt(userGroup.getId(), 0));
		request.setPerm(permissions);
		request.setSubObjects(subObjects);
		request.setSubGroups(subGroups);

		GenericResponse genericResponse = doSetPermissions(TypePerms.folder,
				ObjectTransformer.getInt(folder.getId(), 0), request);

		ContentNodeTestUtils.assertResponseCode(genericResponse, responseCode);

		TransactionManager.getCurrentTransaction().commit(false);
		assertFolderPermissions(folder, userGroup, permissions);

		return genericResponse;
	}
}
