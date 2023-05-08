package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.db.DBUtils.select;
import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.clearNodeObjectCache;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.data.MapEntry.entry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.response.UserList;
import com.gentics.contentnode.rest.model.response.UserListResponse;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for getting user lists
 */
public class UserListTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static UserGroup testGroup;
	private static UserGroup subGroup;
	private static SystemUser testUser;
	private static SystemUser subUser;
	private static SystemUser bothGroupsUser;

	@BeforeClass
	public static void setupOnce() throws NodeException, PortalCacheException {
		testContext.getContext().getTransaction().commit();

		// make sure every user is at least member of one group
		int groupId = supply(() -> DBUtils.select("SELECT MAX(id) FROM usergroup", rs -> {
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				return 0;
			}
		}));
		assertThat(groupId).as("Group ID").isPositive();

		operate(() -> DBUtils.update("INSERT IGNORE INTO user_group (user_id, usergroup_id) SELECT id, ? FROM systemuser", groupId));

		clearNodeObjectCache();

		testGroup = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		subGroup = supply(() -> createUserGroup("Subgroup", testGroup.getId()));

		testUser = supply(() -> createSystemUser("test", "test", null, "testuser1", "testuser", Arrays.asList(testGroup)));
		subUser = supply(() -> createSystemUser("test", "test", null, "testuser2", "testuser", Arrays.asList(subGroup)));
		bothGroupsUser = supply(() -> createSystemUser("test", "test", null, "testuser3", "testuser", Arrays.asList(testGroup, subGroup)));
	}

	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> DBUtils.update("UPDATE systemuser SET active = ?", 1));
	}

	/**
	 * Test getting all users
	 * @throws Exception
	 */
	@Test
	public void testUserList() throws Exception {
		Set<Integer> ids = supply(() -> select("SELECT id FROM systemuser WHERE active = 1", DBUtils.IDS));

		UserList userlist = supply(() -> new UserResourceImpl().list(null, null, null, null, null));
		assertResponseOK(userlist);

		assertThat(userlist.getItems().stream().map(User::getId).collect(Collectors.toList())).as("Returned user IDs").containsOnlyElementsOf(ids);
	}

	/**
	 * Test that deactivated users are not returned in the full list
	 * @throws Exception
	 */
	@Test
	public void testDeactivatedUser() throws Exception {
		Set<Integer> ids = supply(() -> select("SELECT id FROM systemuser WHERE active = 1", DBUtils.IDS));

		int deactivatedUserId = ids.stream().max(Integer::compare).orElse(0);
		assertTrue("Could not find user to deactivate", deactivatedUserId > 0);

		operate(() -> update("UPDATE systemuser SET active = ? WHERE id = ?", 0, deactivatedUserId));
		ids.remove(deactivatedUserId);

		clearNodeObjectCache();

		UserListResponse userlist = supply(() -> new UserResourceImpl().list(0, -1, null, null, null, null, null, null, null, null, null, false));
		assertResponseOK(userlist);

		assertThat(userlist.getUsers().stream().map(User::getId).collect(Collectors.toList())).as("Returned user IDs").containsOnlyElementsOf(ids);
	}

	/**
	 * Test attaching permissions
	 * @throws NodeException
	 */
	@Test
	public void testGetPermissions() throws NodeException {
		PermsParameterBean perms = new PermsParameterBean();
		perms.perms = true;
		UserList userlist = supply(testUser, () -> new UserResourceImpl().list(null, null, null, perms, null));
		assertThat(userlist.getItems().stream().map(User::getId).collect(Collectors.toList())).as("Returned user IDs").containsOnly(testUser.getId(),
				subUser.getId(), bothGroupsUser.getId());
		assertThat(userlist.getPerms()).containsOnly(perms(testUser, Permission.view, Permission.edit),
				perms(subUser, Permission.view, Permission.edit, Permission.delete), perms(bothGroupsUser, Permission.view));

		userlist = supply(subUser, () -> new UserResourceImpl().list(null, null, null, perms, null));
		assertThat(userlist.getItems().stream().map(User::getId).collect(Collectors.toList())).as("Returned user IDs").containsOnly(subUser.getId(),
				bothGroupsUser.getId());
		assertThat(userlist.getPerms()).containsOnly(perms(subUser, Permission.view, Permission.edit), perms(bothGroupsUser, Permission.view));

		userlist = supply(bothGroupsUser, () -> new UserResourceImpl().list(null, null, null, perms, null));
		assertThat(userlist.getItems().stream().map(User::getId).collect(Collectors.toList())).as("Returned user IDs").containsOnly(testUser.getId(),
				subUser.getId(), bothGroupsUser.getId());
		assertThat(userlist.getPerms()).containsOnly(perms(testUser, Permission.view),
				perms(subUser, Permission.view, Permission.edit, Permission.delete), perms(bothGroupsUser, Permission.view, Permission.edit));
	}

	/**
	 * Create an entry, that maps the user id to the set of permissions
	 * @param user user
	 * @param perms permissions
	 * @return map entry
	 */
	protected Map.Entry<Integer, Set<Permission>> perms(SystemUser user, Permission...perms) {
		return entry(user.getId(), new HashSet<>(Arrays.asList(perms)));
	}
}
