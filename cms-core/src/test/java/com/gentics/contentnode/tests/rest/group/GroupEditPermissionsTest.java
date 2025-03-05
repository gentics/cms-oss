package com.gentics.contentnode.tests.rest.group;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.response.UserList;
import com.gentics.contentnode.rest.model.response.UserListResponse;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;
import com.gentics.contentnode.testutils.Creator;

/**
 * Test cases for permission handling when updating groups
 */
@RunWith(value = Parameterized.class)
public class GroupEditPermissionsTest extends AbstractGroupEditTest {
	/**
	 * Node Group
	 */
	private UserGroup nodeGroup;

	/**
	 * Test Group
	 */
	private UserGroup testGroup;

	/**
	 * Test User
	 */
	private SystemUser testUser;

	/**
	 * Sub Group of the Test Group
	 */
	private UserGroup subGroup;

	/**
	 * "Side" Group of the Test Group (sibling)
	 */
	private UserGroup sideGroup;

	@Parameter(0)
	public TestedGroup tested;

	@Parameter(1)
	public boolean adminView;

	@Parameter(2)
	public boolean groupsView;

	@Parameter(3)
	public boolean create;

	@Parameter(4)
	public boolean update;

	@Parameter(5)
	public boolean delete;

	@Parameter(6)
	public boolean useradd;

	@Parameter(7)
	public boolean userupdate;

	@Parameter(8)
	public boolean perm;

	protected boolean canView;

	/**
	 * Return test parameters
	 * @return test parameters
	 */
	@Parameters(name = "{index}: test {0}, adminview {1}, groupsview {2}, create {3}, update {4}, delete {5}, useradd {6}, userupdate {7}, perm {8}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		// missing admin perm (all other flags irrelevant)
		data.add(new Object[] { TestedGroup.subgroup, false, true, true, true, true, true, true, true });

		// missing group admin perm (all other flags irrelevant)
		data.add(new Object[] { TestedGroup.subgroup, true, false, true, true, true, true, true, true });

		for (TestedGroup tested : TestedGroup.values()) {
			for (boolean create : Arrays.asList(true, false)) {
				for (boolean update : Arrays.asList(true, false)) {
					for (boolean delete : Arrays.asList(true, false)) {
						for (boolean useradd : Arrays.asList(true, false)) {
							for (boolean userupdate : Arrays.asList(true, false)) {
								for (boolean perm : Arrays.asList(true, false)) {
									data.add(new Object[] { tested, true, true, create, update, delete, useradd, userupdate, perm });
								}
							}
						}
					}
				}
			}
		}

		return data;
	}

	/**
	 * Create test group and user. Set tested permissions
	 * @throws NodeException
	 */
	@Before
	public void setupUserAndGroup() throws NodeException {
		nodeGroup = Trx.supply(t -> t.getObject(UserGroup.class, NODE_GROUP));
		testGroup = Trx.supply(() -> Creator.createUsergroup("Test Group", "", nodeGroup));
		testUser = Trx.supply(() -> Creator.createUser("tester", "tester", "Test", "er", "", Arrays.asList(testGroup)));

		// set permissions on test group
		Permission adminPerm = new Permission(PermHandler.EMPTY_PERM);
		if (adminView) {
			adminPerm.mergeBits(new Permission(PermHandler.PERM_VIEW).toString());
		}
		Trx.operate(() -> PermHandler.setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(testGroup), adminPerm.toString()));

		Permission groupPerm = new Permission(PermHandler.EMPTY_PERM);
		if (groupsView) {
			groupPerm.mergeBits(new Permission(PermHandler.PERM_VIEW).toString());
		}
		if (create) {
			groupPerm.mergeBits(new Permission(PermHandler.PERM_GROUP_CREATE).toString());
		}
		if (update) {
			groupPerm.mergeBits(new Permission(PermHandler.PERM_GROUP_UPDATE).toString());
		}
		if (delete) {
			groupPerm.mergeBits(new Permission(PermHandler.PERM_GROUP_DELETE).toString());
		}
		if (useradd) {
			groupPerm.mergeBits(new Permission(PermHandler.PERM_GROUP_USERADD).toString());
		}
		if (userupdate) {
			groupPerm.mergeBits(new Permission(PermHandler.PERM_GROUP_USERUPDATE).toString());
		}
		if (perm) {
			groupPerm.mergeBits(new Permission(PermHandler.PERM_CHANGE_GROUP_PERM).toString());
		}
		Trx.operate(() -> PermHandler.setPermissions(UserGroup.TYPE_GROUPADMIN, Arrays.asList(testGroup), groupPerm.toString()));

		// all permissions on users
		Trx.operate(() -> PermHandler.setPermissions(SystemUser.TYPE_USERADMIN, Arrays.asList(testGroup),
				new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_USER_CREATE, PermHandler.PERM_USER_UPDATE, PermHandler.PERM_USER_DELETE).toString()));

		subGroup = Trx.supply(() -> Creator.createUsergroup("Sub Group", "", testGroup));
		sideGroup = Trx.supply(() -> Creator.createUsergroup("Side Group", "", nodeGroup));

		canView = (adminView && groupsView && tested.isOwnOrSub());
	}

	/**
	 * Test reading
	 * @throws NodeException
	 */
	@Test
	public void testRead() throws NodeException {
		UserGroup testedGroup = getTestedGroup();
		if (!adminView || !groupsView || !tested.isOwnOrSub()) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		try (Trx trx = new Trx(testUser)) {
			Group ownGroup = new GroupResourceImpl().get(String.valueOf(testedGroup.getId())).getGroup();
			assertThat(ownGroup).as("Read group").has(attribute("id", testedGroup.getId()));
			trx.success();
		}
	}

	/**
	 * Test reading the subgroups
	 * @throws NodeException
	 */
	@Test
	public void testGetSubgroups() throws NodeException {
		UserGroup testedGroup = getTestedGroup();
		if (!adminView || !groupsView || !tested.isOwnOrSub()) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		try (Trx trx = new Trx(testUser)) {
			new GroupResourceImpl().subgroups(String.valueOf(testedGroup.getId()), null, null, null, null);
			trx.success();
		}
	}

	/**
	 * Test updating
	 * @throws NodeException
	 */
	@Test
	public void testUpdate() throws NodeException {
		UserGroup testedGroup = getTestedGroup();
		if (!adminView || !groupsView || !update || !tested.isSub()) {
			if (canView) {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %s (%d).", testedGroup.getName(), testedGroup.getId()));
			} else {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
			}
		}
		try (Trx trx = new Trx(testUser)) {
			new GroupResourceImpl().update(String.valueOf(testedGroup.getId()), new Group().setDescription("bla"));
			trx.success();
		}
	}

	/**
	 * Test deleting
	 * @throws NodeException
	 */
	@Test
	public void testDelete() throws NodeException {
		UserGroup testedGroup = getTestedGroup();
		if (!adminView || !groupsView || !delete || !tested.isSub()) {
			if (canView) {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %s (%d).", testedGroup.getName(), testedGroup.getId()));
			} else {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
			}
		}
		try (Trx trx = new Trx(testUser)) {
			new GroupResourceImpl().delete(String.valueOf(testedGroup.getId()));
			trx.success();
		}
	}

	/**
	 * Test creating a subgroup
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws NodeException {
		UserGroup testedGroup = getTestedGroup();
		if (!adminView || !groupsView || !create || !tested.isOwnOrSub()) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		try (Trx trx = new Trx(testUser)) {
			new GroupResourceImpl().add(String.valueOf(testedGroup.getId()), new Group().setName("bla"));
			trx.success();
		}
	}

	/**
	 * Test creating a user in the group
	 * @throws NodeException
	 */
	@Test
	public void testCreateUser() throws NodeException {
		UserGroup testedGroup = getTestedGroup();
		if (!adminView || !groupsView || !useradd || !tested.isSub()) {
			if (canView) {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %s (%d).", testedGroup.getName(), testedGroup.getId()));
			} else {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
			}
		}
		try (Trx trx = new Trx(testUser)) {
			UserListResponse list = new UserResourceImpl().list(0, -1, null, Arrays.asList("username"), null, null, null, null, null, null, null, false);
			assertThat(list.getUsers()).as("Users before creating").isEmpty();
			trx.success();
		}
		User newUser = null;
		try (Trx trx = new Trx(testUser)) {
			newUser = new GroupResourceImpl().createUser(String.valueOf(testedGroup.getId()), new User().setLogin("username")).getUser();
			trx.success();
		}
		try (Trx trx = new Trx(testUser)) {
			UserListResponse list = new UserResourceImpl().list(0, -1, null, Arrays.asList("username"), null, null, null, null, null, null, null, false);
			assertThat(list.getUsers()).as("Users after adding").usingFieldByFieldElementComparator().containsOnly(newUser);
			trx.success();
		}
	}

	/**
	 * Test updating a user in the group
	 * @throws NodeException
	 */
	@Test
	public void testUpdateUser() throws NodeException {
		UserGroup testedGroup = getTestedGroup();
		User newUser = Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(testedGroup.getId()), new User().setLogin("username")).getUser();
		});
		if (!adminView || !userupdate || !tested.isSub()) {
			if (adminView && tested.isOwnOrSub()) {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für den Benutzer %s (%d).", newUser.getLogin(), newUser.getId()));
			} else {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für den Benutzer %d.", newUser.getId()));
			}
		}

		try (Trx trx = new Trx(testUser)) {
			new UserResourceImpl().update(String.valueOf(newUser.getId()), new User().setDescription("bla"));
			trx.success();
		}
	}

	/**
	 * Test listing users of the group
	 * @throws NodeException
	 */
	@Test
	public void testListUsers() throws NodeException {
		UserGroup testedGroup = getTestedGroup();
		User newUser = Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(testedGroup.getId()), new User().setLogin("username")).getUser();
		});
		if (!adminView || !groupsView || !tested.isOwnOrSub()) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		try (Trx trx = new Trx(testUser)) {
			List<User> members = new GroupResourceImpl().users(String.valueOf(testedGroup.getId()), null, null, null, null, null).getItems();
			assertThat(members).as("Group members").usingFieldByFieldElementComparator().contains(newUser);
			trx.success();
		}
	}

	/**
	 * Test adding an (existing) user in the group
	 * @throws NodeException
	 */
	@Test
	public void testAddUser() throws NodeException {
		User me = supply(testUser, () -> new UserResourceImpl().getMe(false).getUser());
		UserGroup testedGroup = getTestedGroup();

		// create group and user, the testuser can administrate (group is subgroup of testGroup)
		Group foreignGroup = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(testGroup.getId()), new Group().setName("Foreign Group")).getGroup();
		});
		User newUser = Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(foreignGroup.getId()), new User().setLogin("username")).getUser();
		});

		if (!adminView || !groupsView || !useradd || !tested.isSub()) {
			if (canView) {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %s (%d).", testedGroup.getName(), testedGroup.getId()));
			} else {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
			}
		}

		try (Trx trx = new Trx(testUser)) {
			UserList userlist = new GroupResourceImpl().users(Integer.toString(testedGroup.getId()), null, null, null, null, null);
			if (tested == TestedGroup.ownGroup) {
				assertThat(userlist.getItems()).as("Users before adding").usingFieldByFieldElementComparator().containsOnly(me);
			} else {
				assertThat(userlist.getItems()).as("Users before adding").isEmpty();
			}
			trx.success();
		}

		try (Trx trx = new Trx(testUser)) {
			new GroupResourceImpl().addUser(String.valueOf(testedGroup.getId()), String.valueOf(newUser.getId()));
			trx.success();
		}

		try (Trx trx = new Trx(testUser)) {
			UserList userlist = new GroupResourceImpl().users(Integer.toString(testedGroup.getId()), null, null, null, null, null);
			assertThat(userlist.getItems()).as("Users before adding").usingFieldByFieldElementComparator().containsOnly(newUser);
			trx.success();
		}
	}

	/**
	 * Test adding an (existing) user in the group using the user resource
	 * @throws NodeException
	 */
	@Test
	public void testAddUser2() throws NodeException {
		UserGroup testedGroup = getTestedGroup();

		// create group and user, the testuser can administrate (group is subgroup of testGroup)
		Group foreignGroup = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(testGroup.getId()), new Group().setName("Foreign Group")).getGroup();
		});
		User newUser = Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(foreignGroup.getId()), new User().setLogin("username")).getUser();
		});

		if (!adminView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für den Benutzer %d.", newUser.getId()));
		} else if (!groupsView || !useradd || !tested.isSub()) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}

		try (Trx trx = new Trx(testUser)) {
			new UserResourceImpl().addToGroup(String.valueOf(newUser.getId()), String.valueOf(testedGroup.getId()));
			trx.success();
		}
	}

	/**
	 * Test removing a user from the group
	 * @throws NodeException
	 */
	@Test
	public void testRemoveUser() throws NodeException {
		User me = supply(testUser, () -> new UserResourceImpl().getMe(false).getUser());
		UserGroup testedGroup = getTestedGroup();

		// create group and user, the testuser can administrate (group is subgroup of testGroup)
		Group foreignGroup = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(testGroup.getId()), new Group().setName("Foreign Group")).getGroup();
		});
		User newUser = Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(foreignGroup.getId()), new User().setLogin("username")).getUser();
		});

		// add new user to tested group
		Trx.operate(() -> {
			new GroupResourceImpl().addUser(String.valueOf(testedGroup.getId()), String.valueOf(newUser.getId()));
		});

		if (!adminView || !groupsView || !useradd || !tested.isSub()) {
			if (canView) {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %s (%d).", testedGroup.getName(), testedGroup.getId()));
			} else {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
			}
		}

		try (Trx trx = new Trx(testUser)) {
			UserList userlist = new GroupResourceImpl().users(Integer.toString(testedGroup.getId()), null, null, null, null, null);
			if (tested == TestedGroup.ownGroup) {
				assertThat(userlist.getItems()).as("Users before removing").usingFieldByFieldElementComparator().containsOnly(newUser, me);
			} else {
				assertThat(userlist.getItems()).as("Users before removing").usingFieldByFieldElementComparator().containsOnly(newUser);
			}
			trx.success();
		}

		try (Trx trx = new Trx(testUser)) {
			new GroupResourceImpl().removeUser(String.valueOf(testedGroup.getId()), String.valueOf(newUser.getId()));
			trx.success();
		}

		try (Trx trx = new Trx(testUser)) {
			UserList userlist = new GroupResourceImpl().users(Integer.toString(testedGroup.getId()), null, null, null, null, null);
			assertThat(userlist.getItems()).as("Users after removing").isEmpty();
			trx.success();
		}
	}

	/**
	 * Test removing a user from the group using the user resource
	 * @throws NodeException
	 */
	@Test
	public void testRemoveUser2() throws NodeException {
		UserGroup testedGroup = getTestedGroup();

		// create group and user, the testuser can administrate (group is subgroup of testGroup)
		Group foreignGroup = Trx.supply(() -> {
			return new GroupResourceImpl().add(String.valueOf(testGroup.getId()), new Group().setName("Foreign Group")).getGroup();
		});
		User newUser = Trx.supply(() -> {
			return new GroupResourceImpl().createUser(String.valueOf(foreignGroup.getId()), new User().setLogin("username")).getUser();
		});

		// add new user to tested group
		Trx.operate(() -> {
			new GroupResourceImpl().addUser(String.valueOf(testedGroup.getId()), String.valueOf(newUser.getId()));
		});

		if (!adminView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für den Benutzer %d.", newUser.getId()));
		} else if (!groupsView || !useradd || !tested.isSub()) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}

		try (Trx trx = new Trx(testUser)) {
			new UserResourceImpl().removeFromGroup(String.valueOf(newUser.getId()), String.valueOf(testedGroup.getId()));
			trx.success();
		}
	}

	/**
	 * Test moving the tested group into a target group
	 * @throws NodeException
	 */
	@Test
	public void testMoveFrom() throws NodeException {
		UserGroup targetGroup = Trx.supply(() -> Creator.createUsergroup("Target Group", "", testGroup));

		UserGroup testedGroup = getTestedGroup();

		if (!adminView || !groupsView || !delete || !create || !tested.isSub()) {
			if (canView) {
				if (adminView && groupsView && delete && !create && tested.isSub()) {
					exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %s (%d).", targetGroup.getName(), targetGroup.getId()));
				} else {
					exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %s (%d).", testedGroup.getName(), testedGroup.getId()));
				}
			} else {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
			}
		}

		try (Trx trx = new Trx(testUser)) {
			new GroupResourceImpl().move(String.valueOf(targetGroup.getId()), String.valueOf(testedGroup.getId()));
			trx.success();
		}
	}

	/**
	 * Test moving a source group into the tested group
	 * @throws NodeException
	 */
	@Test
	public void testMoveTo() throws NodeException {
		// if the tested group is the test group itself, the test makes no sense
		if (tested.isOwn()) {
			return;
		}

		UserGroup movedGroup = Trx.supply(() -> Creator.createUsergroup("Moved Group", "", testGroup));

		UserGroup testedGroup = getTestedGroup();

		if (!adminView || !groupsView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", movedGroup.getId()));
		} else if (!delete) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %s (%d).", movedGroup.getName(), movedGroup.getId()));
		} else if (!create || !tested.isSub()) {
			if (canView) {
				if (!create && tested.isSub()) {
					exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %s (%d).", testedGroup.getName(), testedGroup.getId()));
				} else {
					exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %s (%d).", movedGroup.getName(), movedGroup.getId()));
				}
			} else {
				exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
			}
		}

		try (Trx trx = new Trx(testUser)) {
			new GroupResourceImpl().move(String.valueOf(testedGroup.getId()), String.valueOf(movedGroup.getId()));
			trx.success();
		}
	}

	/**
	 * Get the tested group
	 * @return tested group
	 */
	protected UserGroup getTestedGroup() {
		switch (tested) {
		case superGroup:
		default:
			return nodeGroup;
		case ownGroup:
			return testGroup;
		case subgroup:
			return subGroup;
		case sideGroup:
			return sideGroup;
		}
	}

	/**
	 * Possible tested groups
	 */
	public static enum TestedGroup {
		superGroup(false, false),
		ownGroup(true, false),
		subgroup(false, true),
		sideGroup(false, false);

		private boolean own;

		private boolean sub;

		TestedGroup(boolean own, boolean sub) {
			this.own = own;
			this.sub = sub;
		}

		/**
		 * Return true, if the group is a subgroup of the user's group
		 * @return true for subgroups
		 */
		public boolean isSub() {
			return sub;
		}

		/**
		 * Return true for the user's own group
		 * @return true for own group
		 */
		public boolean isOwn() {
			return own;
		}

		/**
		 * Return true, if the group is the user's own group or a subgroup
		 * @return true for own and subgroups
		 */
		public boolean isOwnOrSub() {
			return own || sub;
		}

		/**
		 * Return true, if the group is the user's super group
		 * @return true for the supergroup
		 */
		public boolean isSuper() {
			return !own && !sub;
		}
	}
}
