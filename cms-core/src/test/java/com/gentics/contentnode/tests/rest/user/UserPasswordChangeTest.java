package com.gentics.contentnode.tests.rest.user;

import static com.gentics.contentnode.db.DBUtils.executeUpdate;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.client.Entity;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.filter.AbstractSSOFilter;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.request.ChangePasswordRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

/**
 * Tests for changing the password of the current user with POST /user/password
 */
public class UserPasswordChangeTest {
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

	/**
	 * Old password of the test users
	 */
	private static final String OLD_PASSWORD = "oldpassword";

	/**
	 * New password set by the tests
	 */
	private static final String NEW_PASSWORD = "newpassword";

	/**
	 * Counter for unique logins
	 */
	private static final AtomicInteger userCounter = new AtomicInteger();

	/**
	 * Group without any permissions
	 */
	private static UserGroup group;

	/**
	 * Create the group without permissions
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("NoPermissionGroup", NODE_GROUP_ID));
		// createUserGroup copies the permissions of the mother group, so remove them all
		operate(() -> executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] { group.getId() }));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(group.getId()));
	}

	/**
	 * Test changing the own password via REST as user without any permissions
	 * @throws NodeException
	 * @throws RestException
	 */
	@Test
	public void testChangePasswordWithoutPermissions() throws NodeException, RestException {
		SystemUser user = createUser();

		GenericResponse response;
		try (LoggedInClient client = restContext.client(user.getLogin(), OLD_PASSWORD)) {
			response = client.get().base().path("user").path("password").request()
					.post(Entity.json(new ChangePasswordRequest().setOldPassword(OLD_PASSWORD).setNewPassword(NEW_PASSWORD)), GenericResponse.class);
		}

		assertResponseOK(response);
		assertThat(passwordMatches(user, NEW_PASSWORD)).as("New password matches").isTrue();
		assertThat(passwordMatches(user, OLD_PASSWORD)).as("Old password matches").isFalse();
	}

	/**
	 * Test that changing the password fails, if the old password is wrong
	 * @throws NodeException
	 */
	@Test
	public void testWrongOldPassword() throws NodeException {
		SystemUser user = createUser();

		GenericResponse response = changePassword(user, "wrongpassword", NEW_PASSWORD);

		assertResponse(response, ResponseCode.INVALIDDATA, "Old password does not match");
		assertThat(response.getMessages()).as("Response messages").hasSize(1);
		assertThat(response.getMessages().get(0).getFieldName()).as("Field of message").isEqualTo("oldPassword");
		assertPasswordUnchanged(user);
	}

	/**
	 * Test that changing the password fails, if the old password is missing
	 * @throws NodeException
	 */
	@Test
	public void testMissingOldPassword() throws NodeException {
		SystemUser user = createUser();

		GenericResponse response = changePassword(user, null, NEW_PASSWORD);

		assertResponse(response, ResponseCode.INVALIDDATA, "Old and new password must be given");
		assertPasswordUnchanged(user);
	}

	/**
	 * Test that changing the password fails, if the new password is empty
	 * @throws NodeException
	 */
	@Test
	public void testEmptyNewPassword() throws NodeException {
		SystemUser user = createUser();

		GenericResponse response = changePassword(user, OLD_PASSWORD, "");

		assertResponse(response, ResponseCode.INVALIDDATA, "Old and new password must be given");
		assertPasswordUnchanged(user);
	}

	/**
	 * Test that the password of a user created by single sign-on (e.g. Keycloak) cannot be changed
	 * @throws NodeException
	 */
	@Test
	public void testSsoUser() throws NodeException {
		SystemUser user = createUser();
		operate(t -> {
			SystemUser editable = t.getObject(user, true);
			editable.setPassword(AbstractSSOFilter.SSO_PASSWORD);
			editable.save();
		});

		GenericResponse response = changePassword(user, AbstractSSOFilter.SSO_PASSWORD, NEW_PASSWORD);

		assertResponse(response, ResponseCode.FAILURE, "Cannot change password of single sign-on user");
		String storedPassword = supply(t -> t.getObject(SystemUser.class, user.getId()).getPassword());
		assertThat(storedPassword).as("Stored password").isEqualTo(AbstractSSOFilter.SSO_PASSWORD);
	}

	/**
	 * Test that the password of a support user cannot be changed
	 * @throws NodeException
	 */
	@Test
	public void testSupportUser() throws NodeException {
		// the support user flag is only persisted when the user is created, so it must be set before the first save
		SystemUser user = supply(t -> {
			SystemUser newUser = t.createObject(SystemUser.class);
			newUser.setActive(true);
			newUser.setFirstname("Support");
			newUser.setLastname("User");
			newUser.setLogin("pwchange" + userCounter.incrementAndGet());
			// store a valid hash of the old password, so the request would succeed if the user were no support user
			newUser.setPassword(SystemUserFactory.hashPassword(OLD_PASSWORD, 0));
			newUser.setSupportUser(true);
			newUser.getUserGroups().add(group);
			newUser.save();
			return t.getObject(SystemUser.class, newUser.getId());
		});
		assertThat(user.isSupportUser()).as("Is support user").isTrue();

		GenericResponse response = changePassword(user, OLD_PASSWORD, NEW_PASSWORD);

		assertResponse(response, ResponseCode.FAILURE, "Cannot change password of support user");
		assertPasswordUnchanged(user);
	}

	/**
	 * Create a new user in the group without permissions, with password {@link #OLD_PASSWORD}
	 * @return user
	 * @throws NodeException
	 */
	protected SystemUser createUser() throws NodeException {
		String login = "pwchange" + userCounter.incrementAndGet();
		return supply(() -> createSystemUser("Password", "Changer", null, login, OLD_PASSWORD, Arrays.asList(group)));
	}

	/**
	 * Call the endpoint as the given user
	 * @param user user
	 * @param oldPassword old password
	 * @param newPassword new password
	 * @return response
	 * @throws NodeException
	 */
	protected GenericResponse changePassword(SystemUser user, String oldPassword, String newPassword) throws NodeException {
		return supply(user, () -> new UserResourceImpl()
				.changePassword(new ChangePasswordRequest().setOldPassword(oldPassword).setNewPassword(newPassword)));
	}

	/**
	 * Check whether the given password matches the stored password of the user
	 * @param user user
	 * @param password password to check
	 * @return true, iff the password matches
	 * @throws NodeException
	 */
	protected boolean passwordMatches(SystemUser user, String password) throws NodeException {
		return supply(t -> ((SystemUserFactory) t.getObjectFactory(SystemUser.class)).getSystemUser(user.getLogin(), password, true) != null);
	}

	/**
	 * Assert that the old password still matches and the new one does not
	 * @param user user
	 * @throws NodeException
	 */
	protected void assertPasswordUnchanged(SystemUser user) throws NodeException {
		assertThat(passwordMatches(user, OLD_PASSWORD)).as("Old password matches").isTrue();
		assertThat(passwordMatches(user, NEW_PASSWORD)).as("New password matches").isFalse();
	}
}
