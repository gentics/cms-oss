package com.gentics.contentnode.tests.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.response.UserLoadResponse;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

public class ClientToServletTest {
	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	/**
	 * Login of the test user
	 */
	private final static String LOGIN = "test";

	/**
	 * Password of the test user
	 */
	private final static String PASSWORD = "test";

	/**
	 * Test user
	 */
	private static SystemUser testUser;

	/**
	 * Create static test data
	 * 
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setUpOnce() throws NodeException {
		// load group
		UserGroup nodeGroup = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<UserGroup> groups = t.getObjects(UserGroup.class, Arrays.asList(2));
			assertThat(groups).as("User Groups").hasSize(1);
			return groups.get(0);
		});

		// create test user
		testUser = Trx.supply(() -> {
			return Creator.createUser(LOGIN, PASSWORD, "Tester", "Tester", "", Arrays.asList(nodeGroup));
		});
	}

	/**
	 * Test login with a single client
	 * @throws Exception
	 */
	@Test
	public void testLogin() throws Exception {
		try (LoggedInClient client1 = restContext.client(LOGIN, PASSWORD)) {
			assertThat(client1.get().getSid()).as("SID client1").isNotNull().isNotEmpty();

			UserLoadResponse user1 = client1.get().base().path("user").path("me").request().get(UserLoadResponse.class);
			client1.get().assertResponse(user1);
			assertThat(user1.getUser().getId()).as("User ID from REST").isEqualTo(testUser.getId());
		}
	}

	/**
	 * Test creating multiple logged in client instances
	 * @throws Exception
	 */
	@Test
	public void testMultipleClients() throws Exception {
		try (LoggedInClient client1 = restContext.client(LOGIN, PASSWORD)) {
			try (LoggedInClient client2 = restContext.client(LOGIN, PASSWORD)) {
				assertThat(client1.get().getSid()).as("SID client1").isNotNull().isNotEmpty();
				assertThat(client2.get().getSid()).as("SID client2").isNotNull().isNotEmpty().isNotEqualTo(client1.get().getSid());

				UserLoadResponse user1 = client1.get().base().path("user").path("me").request().get(UserLoadResponse.class);
				client1.get().assertResponse(user1);
				assertThat(user1.getUser().getId()).as("User ID from REST").isEqualTo(testUser.getId());

				UserLoadResponse user2 = client2.get().base().path("user").path("me").request().get(UserLoadResponse.class);
				client2.get().assertResponse(user2);
				assertThat(user2.getUser().getId()).as("User ID from REST").isEqualTo(testUser.getId());
			}

			UserLoadResponse user1 = client1.get().base().path("user").path("me").request().get(UserLoadResponse.class);
			client1.get().assertResponse(user1);
			assertThat(user1.getUser().getId()).as("User ID from REST").isEqualTo(testUser.getId());
		}
	}

}
