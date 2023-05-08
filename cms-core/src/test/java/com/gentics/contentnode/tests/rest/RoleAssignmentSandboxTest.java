package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.rest.util.MiscUtils.doSetPermissions;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Tests concerning role assignment without PHP code
 * @author escitalopram
 */
@RunWith(Parameterized.class)
public class RoleAssignmentSandboxTest {

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	/**
	 * Set a second role in the same call
	 */
	protected final boolean multipleRoles;

	/**
	 * also set roles on subgroups in the same call
	 */
	protected final boolean setSubgroups;

	/**
	 * also set roles for subobjects in the same call
	 */
	protected final boolean setSubobjects;

	/**
	 * Set the roles on the root folder instead of the folder (only with setSubobjects == true)
	 */
	protected final boolean setOnNode;

	private static int timestampcounter = 0;

	private static ContentLanguage klingon;

	private static ContentLanguage tengwar;

	private static int tengwarRoleId;

	private static int klingonRoleId;

	private Node node;

	private Folder rootFolder;

	private UserGroup mainGroup;

	private UserGroup subGroup;

	private SystemUser mainUser;

	private SystemUser subGroupUser;

	private Folder folder;

	private Folder subfolder;

	private Page klingonPage1;

	private Page klingonPage2;

	private Page tengwarPage1;

	private Page tengwarPage2;

	private static final String FOLDER_PERMS = new Permission(PermHandler.PERM_VIEW).toString();

	public RoleAssignmentSandboxTest(boolean multipleRoles, boolean setSubgroups, boolean setSubobjects, boolean setOnNode) {
		this.multipleRoles = multipleRoles;
		this.setSubgroups = setSubgroups;
		this.setSubobjects = setSubobjects;
		this.setOnNode = setOnNode;
	}

	public static int getSequence() {
		return ++timestampcounter;
	}

	@Parameters(name="{index}: multipleRoles: {0}, setSubgroups: {1}, setSubobjects: {2}, setOnNode: {3}")
	public static List<Object[]> parameters() {
		List<Object[]> result = new ArrayList<Object[]>();
		final boolean[] boolvals = new boolean[] { false, true };
		for (boolean multipleRoles : boolvals) {
			for (boolean setSubgroups : boolvals) {
				for (boolean setSubobjects : boolvals) {
					for (boolean setOnNode : boolvals) {
						if (!setOnNode || setSubobjects) { // can't setOnNode if not setSubobjects
							result.add(new Object[] { multipleRoles, setSubgroups, setSubobjects, setOnNode });
						}
					}
				}
			}
		}
		return result;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.startTransaction(getSequence());

		// Add Languages
		klingon = Creator.createLanguage("Klingon", "tlh");
		tengwar = Creator.createLanguage("Tengwar", "teng");

		// Add Roles
		String showPages = new Permission(PermHandler.ROLE_VIEW).toString();
		Map<ContentLanguage, String> languagePermissions = new HashMap<ContentLanguage, String>();
		languagePermissions.put(klingon, showPages);
		klingonRoleId = Creator.createRole("Klingon", languagePermissions, null, null);

		languagePermissions.clear();
		languagePermissions.put(tengwar, showPages);
		tengwarRoleId = Creator.createRole("Tengwar", languagePermissions, null, null);

	}

	@Before
	public void setup() throws NodeException {
		Transaction t = context.startTransaction(getSequence());

		// Create group with no rights to pages
		UserGroup parentGroup = t.getObject(UserGroup.class, 2);
		mainGroup = Creator.createUsergroup("maingroup" + getSequence(), "", parentGroup);
		subGroup = Creator.createUsergroup("subgroup" + getSequence(), "", mainGroup);

		// Add users
		mainUser = Creator.createUser("mainuser" + getSequence(), "asdf", "first", "last", "e@ma.il", Arrays.asList(new UserGroup[] { mainGroup }));
		subGroupUser = Creator.createUser("subuser" + getSequence(), "asdf", "first", "last", "e@ma.il", Arrays.asList(new UserGroup[] { subGroup }));

		node = Creator.createNode("testnode", "blah", "/", "/", Arrays.asList(new ContentLanguage[] { tengwar, klingon }));
		rootFolder = node.getFolder();

		// Add folders
		folder = Creator.createFolder(rootFolder, "folder", "/");
		subfolder = Creator.createFolder(folder, "subfolder", "/");

		// Set permissions
		List<UserGroup> allGroups = Arrays.asList(new UserGroup[] { mainGroup, subGroup });
		PermHandler.setPermissions(Folder.TYPE_FOLDER, rootFolder.getId(), allGroups, FOLDER_PERMS);
		PermHandler.setPermissions(Node.TYPE_NODE, rootFolder.getId(), allGroups, FOLDER_PERMS);
		PermHandler.setPermissions(Folder.TYPE_FOLDER, folder.getId(), allGroups, FOLDER_PERMS);
		PermHandler.setPermissions(Folder.TYPE_FOLDER, subfolder.getId(), allGroups, FOLDER_PERMS);

		// Add some pages
		Template template = Creator.createTemplate("testtemplate", "this is a test", rootFolder);
		klingonPage1 = Creator.createPage("klingon1", folder, template, klingon);
		klingonPage2 = Creator.createPage("klingon2", subfolder, template, klingon);
		tengwarPage1 = Creator.createPage("tengwar1", folder, template, tengwar);
		tengwarPage2 = Creator.createPage("tengwar2", subfolder, template, tengwar);
		PermissionStore.initialize(true);
	}

	/**
	 * Test whether changing roles enabled for a folder via REST-API has the desired consequences.
	 * @throws NodeException
	 */
	@Test
	public void testRoleAssignment() throws NodeException {
		context.startSystemUserTransaction(getSequence(), true);
		TransactionManager.getCurrentTransaction().dirtObjectCache(SystemUser.class, 1);
		TransactionManager.getCurrentTransaction().dirtObjectCache(UserGroup.class, 2);
		// check access
		Page[] allPages = new Page[] { klingonPage1, klingonPage2, tengwarPage1, tengwarPage2 };
		for (Page p : allPages) {
			assertPageReadableByUser(p, mainUser, false);
			assertPageReadableByUser(p, subGroupUser, false);
		}

		// set role
		GenericResponse result = setRoles(true);

		assertEquals("Setting roles failed", ResponseCode.OK, result.getResponseInfo().getResponseCode());

		// check access
		context.startSystemUserTransaction(getSequence(), true);

		assertPageReadableByUser(klingonPage1, mainUser, true);
		assertPageReadableByUser(klingonPage1, subGroupUser, setSubgroups);
		assertPageReadableByUser(klingonPage2, mainUser, setSubobjects);
		assertPageReadableByUser(klingonPage2, subGroupUser, setSubobjects && setSubgroups);

		assertPageReadableByUser(tengwarPage1, mainUser, multipleRoles);
		assertPageReadableByUser(tengwarPage1, subGroupUser, multipleRoles && setSubgroups);
		assertPageReadableByUser(tengwarPage2, mainUser, multipleRoles && setSubobjects);
		assertPageReadableByUser(tengwarPage2, subGroupUser, multipleRoles && setSubobjects && setSubgroups);

		// unset role
		result = setRoles(false);

		assertEquals("Setting roles failed", ResponseCode.OK, result.getResponseInfo().getResponseCode());

		// check access
		context.startSystemUserTransaction(getSequence(), true);

		for (Page p : allPages) {
			assertPageReadableByUser(p, mainUser, false);
			assertPageReadableByUser(p, subGroupUser, false);
		}
	}

	/**
	 * Enabled or disable roles on the test folders according to the test settings
	 * @param enable whether to enable or disable the roles
	 * @return response from REST-API
	 * @throws NodeException
	 */
	private GenericResponse setRoles(boolean enable) throws NodeException {
		context.startSystemUserTransaction(getSequence(), true);

		int objId = (setOnNode ? rootFolder.getId() : folder.getId());
		SetPermsRequest req = new SetPermsRequest();
		req.setPerm(FOLDER_PERMS);
		req.setGroupId(mainGroup.getId());
		Set<Integer> roleset = new HashSet<Integer>();
		if (enable) {
			roleset.add(klingonRoleId);

			if (multipleRoles) {
				roleset.add(tengwarRoleId);
			}
		}

		req.setRoleIds(roleset);
		req.setSubGroups(setSubgroups);
		req.setSubObjects(setSubobjects);

		GenericResponse result = doSetPermissions(TypePerms.folder, objId, req);
		return result;
	}

	/**
	 * Check if a page has the correct readability for a specified user. Commits the current transaction and starts a new one
	 * @param page the page to check
	 * @param user the user to check
	 * @param shouldBeViewable whether the page should be readable or not
	 * @throws NodeException
	 */
	public static void assertPageReadableByUser(final Page page, final SystemUser user, final boolean shouldBeViewable) throws NodeException {
		context.getContext().startTransaction(user.getId());
		Transaction t = TransactionManager.getCurrentTransaction();
		t.setTimestamp(getSequence());
		boolean actuallyViewable = t.getPermHandler().canView(page);
		assertEquals("Permission check failed", shouldBeViewable, actuallyViewable);
		context.startTransaction(getSequence());
	}
}
