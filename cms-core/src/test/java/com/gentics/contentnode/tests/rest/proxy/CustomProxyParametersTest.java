package com.gentics.contentnode.tests.rest.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.BadRequestException;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

/**
 * Test cases for custom proxy
 */
public class CustomProxyParametersTest {
	public final static String RESOURCE_KEY = "myresource";

	public final static String UNKNOWN_RESOURCE_KEY = "unknown";

	/**
	 * Custom app context
	 */
	private static RESTAppContext customAppContext = new RESTAppContext(new ResourceConfig(CustomToolResource.class)).baseUriPattern("http://localhost:%d/")
			.startPort(9080);

	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext().config(CustomProxyParametersTest.class.getResource("custom_proxy.yml").getFile()).config(prefs -> {
		// set the URL of the custom app
		prefs.set("custom_proxy." + RESOURCE_KEY + ".baseUrl", customAppContext.getBaseUri() + "tool/variable/{{param}}");
	});

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(customAppContext).around(testContext).around(restContext);

	/**
	 * Login of the test user with permission
	 */
	private final static String LOGIN_WITH = "testwith";

	/**
	 * Login of the test user without permission
	 */
	private final static String LOGIN_WITHOUT = "testwithout";

	/**
	 * Password of the test users
	 */
	private final static String PASSWORD = "test";

	@BeforeClass
	public static void setUpOnce() throws NodeException {
		// load group
		UserGroup nodeGroup = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<UserGroup> groups = t.getObjects(UserGroup.class, Arrays.asList(2));
			assertThat(groups).as("User Groups").hasSize(1);
			return groups.get(0);
		});

		// create test user with permission
		Trx.supply(() -> {
			return Creator.createUser(LOGIN_WITH, PASSWORD, "Tester", "Tester", "", Arrays.asList(nodeGroup));
		});

		// create subgroup
		UserGroup subGroup = Trx.supply(() -> Creator.createUsergroup("Subgroup", "", nodeGroup));

		// create test user without permission
		Trx.supply(() -> Creator.createUser(LOGIN_WITHOUT, PASSWORD, "Tester", "Tester", "", Arrays.asList(subGroup)));
	}

	@Test
	public void testDefault() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("path").request().get(String.class)).as("Response")
					.isEqualTo("defaultValue");
		}
	}

	@Test
	public void testValid() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("path").queryParam("param", "validValue").request().get(String.class)).as("Response")
					.isEqualTo("validValue");
		}
	}

	@Test(expected=BadRequestException.class)
	public void testInvalid() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			client.get().base().path("proxy").path(RESOURCE_KEY).path("path").queryParam("param", "invalidValue").request().get(String.class);
		}
	}
}
