package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAllowedException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.response.UsersnapResponse;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

/**
 * Test cases for usersnap integration
 */
@GCNFeature(set = { Feature.USERSNAP })
public class UsersnapResourceTest {
	public final static String ADMIN_LOGIN = "usersnapadmin";

	public final static String ADMIN_PASSWORD = "usersnapadmin";

	public final static String EDITOR_LOGIN = "usersnapeditor";

	public final static String EDITOR_PASSWORD = "usersnapeditor";

	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext().config(UsersnapResourceTest.class.getResource("usersnap.yml").getFile());

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		UserGroup adminGroup = Trx.supply(t -> t.getObject(UserGroup.class, 2));
		assertThat(adminGroup).as("Admin group").isNotNull();

		Trx.supply(() -> Creator.createUser(ADMIN_LOGIN, ADMIN_PASSWORD, "test", "test", "", Arrays.asList(adminGroup)));

		UserGroup editorGroup = Trx.supply(() -> Creator.createUsergroup("editor", "", adminGroup));

		Trx.supply(() -> Creator.createUser(EDITOR_LOGIN, EDITOR_PASSWORD, "editor", "editor", "", Arrays.asList(editorGroup)));
	}

	/**
	 * Test feature off
	 * @throws RestException
	 * @throws NodeException
	 */
	@Test(expected = NotAllowedException.class)
	public void testNoFeature() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, ADMIN_PASSWORD); FeatureClosure fc = new FeatureClosure(Feature.USERSNAP, false)) {
			client.get().base().path("usersnap").request().get(UsersnapResponse.class);
		}
	}

	/**
	 * Test with insufficient privileges
	 * @throws RestException
	 */
	@Test(expected = ForbiddenException.class)
	public void testInsufficientPermission() throws RestException {
		try (LoggedInClient client = restContext.client(EDITOR_LOGIN, EDITOR_PASSWORD)) {
			client.get().base().path("usersnap").request().get(UsersnapResponse.class);
		}
	}

	/**
	 * Test getting settings
	 * @throws RestException
	 */
	@Test
	public void testGet() throws RestException {
		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, ADMIN_PASSWORD)) {
			UsersnapResponse response = client.get().base().path("usersnap").request().get(UsersnapResponse.class);
			assertResponseOK(response);
			assertThat(response.getSettings().toString()).as("Settings").isEqualTo("{\"key\":\"8905e28a-17db-49d2-9f7e-51ac79b608cc\"}");
		}
	}
}
