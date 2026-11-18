package com.gentics.contentnode.tests.rest.user;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.client.Entity;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.client.RestApi;
import com.gentics.contentnode.rest.client.RestClient;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.configuration.KeyProvider;
import com.gentics.contentnode.rest.model.response.PasswordLinkResponse;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

/**
 * Test cases for the authorization of {@code POST /user/{id}/passwordLink}, which issues the token for setting the
 * password of a user
 */
public class PasswordLinkResourceTest {
	public final static String ADMIN_LOGIN = "passwordlinkadmin";

	public final static String ADMIN_PASSWORD = "passwordlinkadmin";

	public final static String EDITOR_LOGIN = "passwordlinkeditor";

	public final static String EDITOR_PASSWORD = "passwordlinkeditor";

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
	 * User, for whom the links are issued
	 */
	private static SystemUser target;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		KeyProvider.init(testContext.getGcnBasePath().getAbsolutePath());

		UserGroup adminGroup = Trx.supply(t -> t.getObject(UserGroup.class, 2));
		assertThat(adminGroup).as("Admin group").isNotNull();

		Trx.supply(() -> Creator.createUser(ADMIN_LOGIN, ADMIN_PASSWORD, "test", "test", "", Arrays.asList(adminGroup)));

		// a subgroup without any permissions, so its members may not administrate users
		UserGroup editorGroup = Trx.supply(() -> Creator.createUsergroup("passwordlinkeditors", "", adminGroup));
		Trx.supply(() -> Creator.createUser(EDITOR_LOGIN, EDITOR_PASSWORD, "editor", "editor", "", Arrays.asList(editorGroup)));

		target = Trx.supply(() -> Creator.createUser("passwordlinktarget", "", "target", "target", "", Arrays.asList(editorGroup)));
	}

	/**
	 * Test that the endpoint is refused without a session
	 * @throws RestException
	 */
	@Test(expected = NotAuthorizedException.class)
	public void testNoLogin() throws RestException {
		post(new RestClient(restContext.getBaseUri()));
	}

	/**
	 * Test that the endpoint is refused for a user without the permission to administrate users
	 * @throws RestException
	 */
	@Test(expected = ForbiddenException.class)
	public void testInsufficientPermission() throws RestException {
		try (LoggedInClient client = restContext.client(EDITOR_LOGIN, EDITOR_PASSWORD)) {
			post(client.get());
		}
	}

	/**
	 * Test that an administrator gets a token with its expiry
	 * @throws RestException
	 */
	@Test
	public void testAdministrator() throws RestException {
		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, ADMIN_PASSWORD)) {
			PasswordLinkResponse response = post(client.get());
			assertResponseOK(response);
			assertThat(response.getToken()).as("Token").isNotBlank();
			assertThat(response.getExpiresAt()).as("Expiry").isGreaterThan((int) (System.currentTimeMillis() / 1000));
		}
	}

	/**
	 * Post the request for a password link of the target user
	 * @param client client
	 * @return response
	 * @throws RestException
	 */
	private static PasswordLinkResponse post(RestApi client) throws RestException {
		return client.base().path("user").path(String.valueOf(target.getId())).path("passwordLink").request()
				.post(Entity.json(""), PasswordLinkResponse.class);
	}
}
