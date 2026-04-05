package com.gentics.contentnode.tests.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.client.RestClient;
import com.gentics.contentnode.rest.client.JerseyRestClientImpl;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

public class SSOClientTest {
	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext(Type.grizzly, ResourceConfig.forApplication(new DummySSOApplication()));

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

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
			return Creator.createUser(DummySSOFilter.LOGIN, DummySSOFilter.PASSWORD, "Tester", "Tester", "", Arrays.asList(nodeGroup));
		});
	}

	/**
	 * Truncate systemsession table for each test
	 * 
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> DBUtils.executeStatement("DELETE FROM systemsession", Transaction.UPDATE_STATEMENT));
	}

	@Test
	public void testSSOLogin() throws Exception {
		RestClient client = new JerseyRestClientImpl(restContext.getBaseUri());
		client.ssologin();
	}
}
