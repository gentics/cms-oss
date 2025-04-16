package com.gentics.contentnode.tests.rest.user;

import java.util.Arrays;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.UserSortAttribute;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.UserList;
import com.gentics.contentnode.rest.model.response.UserListResponse;
import com.gentics.contentnode.rest.model.response.UserLoadResponse;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link UserResource}
 */
public class UserResourceTest {
	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static UserGroup group;

	private static SystemUser user;

	private static SystemUser otherUser;

	private static final String TEST_GROUP_NAME = "TestGroup";

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, NODE_GROUP_ID));
		group = supply(() -> createUserGroup(TEST_GROUP_NAME, NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));
		otherUser = supply(() -> createSystemUser("Other", "User", null, "other", "other", Arrays.asList(group, nodeGroup)));
	}

	@Test
	public void testList() throws NodeException {
		UserList response = assertRequiredPermissions(group, "tester", "tester", restContext,
				target -> target.path("user").request().get(UserList.class),
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(SystemUser.TYPE_USERADMIN, 1, PermHandler.PERM_VIEW));

		assertThat(response.getItems()).as("User list").usingElementComparatorOnFields("id")
				.containsOnly(new User().setId(user.getId()), new User().setId(otherUser.getId()));
	}

	@Test
	public void testLoad() throws NodeException {
		UserLoadResponse response = assertRequiredPermissions(group, "tester", "tester", restContext,
				target -> target.path("user").path(Integer.toString(otherUser.getId())).request()
						.get(UserLoadResponse.class),
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(SystemUser.TYPE_USERADMIN, 1, PermHandler.PERM_VIEW));

		assertThat(response.getUser()).hasFieldOrPropertyWithValue("id", otherUser.getId());
	}

	@Test
	public void givenGetSingleUserRequestWithEmbeddedGroups_shouldReturnUserWithGroups() throws NodeException {
		User retrievedTestUser = supply(() ->
				 new UserResourceImpl().get(user.getId().toString(),
						new EmbedParameterBean().withEmbed("group")).getUser()
		);
		Group retrievedGroup = retrievedTestUser.getGroups().stream()
				.filter(group -> TEST_GROUP_NAME.equals(group.getName()))
				.findAny()
				.get();

		assertThat(retrievedGroup).as("Retrieved user is expected to be in assigned group.").isNotNull();
		assertThat(retrievedGroup).as("Retrieved user is expected to be in assigned group.")
				.hasFieldOrPropertyWithValue("name", TEST_GROUP_NAME);
	}

	@Test
	public void givenUserListRequestWithEmbeddedGroups_shouldReturnUserListWithGroups() throws NodeException {
		UserList userList = supply(() -> new UserResourceImpl().list(null, null, null, null,
				new EmbedParameterBean().withEmbed("group")));

		User retrievedTestUser = userList.getItems()
				.stream()
				.filter(user -> "Tester".equals(user.getFirstName()))
				.findFirst()
				.get();

		Group retrievedGroup = retrievedTestUser.getGroups().stream()
				.filter(group -> TEST_GROUP_NAME.equals(group.getName()))
				.findAny()
				.get();

		assertThat(retrievedGroup).as("Retrieved user is expected to be in assigned group.").isNotNull();
		assertThat(retrievedGroup).as("Retrieved user is expected to be in assigned group.")
				.hasFieldOrPropertyWithValue("name", TEST_GROUP_NAME);

	}

	@Test
	public void testListWithGroups() throws NodeException {
		UserListResponse response = supply(user, () -> new UserResourceImpl().list(0, -1, null, null, null, null, null,
				null, null, UserSortAttribute.id, SortOrder.asc, true));
		assertResponseOK(response);
		assertThat(response.getUsers()).as("User list").usingElementComparatorOnFields("id")
				.containsOnly(new User().setId(user.getId()), new User().setId(otherUser.getId()));

		Group restGroup = supply(() -> UserGroup.TRANSFORM2REST.apply(group));
		for (User user : response.getUsers()) {
			assertThat(user.getGroups()).as("Groups of user " + user.getLogin()).usingElementComparatorOnFields("id").containsOnly(restGroup);
		}
	}

	@Test
	public void testSupportUserPasswordChange() throws NodeException {
		SystemUser supportUser = supply(t -> {
			var nodeGroup = t.getObject(UserGroup.class, 2);
			var newUser = t.createObject(SystemUser.class);

			newUser.setActive(true);
			newUser.setFirstname("Support");
			newUser.setLastname("User");
			newUser.setLogin("support");
			newUser.setPassword("JWT");
			newUser.setSupportUser(true);
			newUser.getUserGroups().add(nodeGroup);

			newUser.save();

			return t.getObject(SystemUser.class, newUser.getId());
		});
		var password = supportUser.getPassword();
		var updateRequest = new User().setPassword("newpassword");
		var updateResponse = supply(supportUser, () -> new UserResourceImpl().update(supportUser.getId().toString(), updateRequest));

		assertResponse(updateResponse, ResponseCode.FAILURE, "Cannot change password of support user");

		var loadedUser = supply(t -> t.getObject(SystemUser.class, supportUser.getId()));

		assertThat(loadedUser.getPassword())
			.as("Support user password")
			.isEqualTo(password);
	}
}
