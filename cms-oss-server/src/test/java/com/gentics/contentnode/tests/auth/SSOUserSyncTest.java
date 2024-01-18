package com.gentics.contentnode.tests.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

public class SSOUserSyncTest {

	@Rule
	public DBTestContext testContext = new DBTestContext().config(map -> {
		map.setProperty("http_auth_login.login", "HTTP_LOGIN");
		map.setProperty("http_auth_login.pw", "HTTP_PW");
		map.setProperty("http_auth_login.firstname", "HTTP_FIRST");
		map.setProperty("http_auth_login.lastname", "HTTP_LAST");
		map.setProperty("http_auth_login.email", "HTTP_MAIL");
		map.setProperty("http_auth_login.group", "HTTP_GROUP");
	});

	/**
	 * Path using the unrestricted filter without group sync for existing users
	 */
	public final static String UNRESTRICTED_NOSYNC_PATH = "/GCN/test/unrestricted/nosync";

	/**
	 * Path using the unrestricted filter with group sync for existing users
	 */
	public final static String UNRESTRICTED_SYNC_PATH = "/GCN/test/unrestricted/sync";

	/**
	 * Path using the restricted filter without group sync for existing users
	 */
	public final static String RESTRICTED_NOSYNC_PATH = "/GCN/test/restricted/nosync";

	/**
	 * Path using the restricted filter with group sync for existing users
	 */
	public final static String RESTRICTED_SYNC_PATH = "/GCN/test/restricted/sync";

	/**
	 * Thread count for the multithreaded test
	 */
	private static final int THREAD_COUNT = 20;

	/**
	 * Servlet runner
	 */
	protected ServletRunner runner;

	/**
	 * Expected restrictions for the unrestricted user
	 */
	protected Map<Integer, Set<Integer>> unrestricted = new HashMap<Integer, Set<Integer>>();

	/**
	 * Expected restrictions for the restricted user
	 */
	protected Map<Integer, Set<Integer>> restricted = new HashMap<Integer, Set<Integer>>();

	/**
	 * Setup the application
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature("http_auth_login", true);

		// setup expected restrictions
		// 5|1;9|2~3
		restricted.put(5, asSet(1));
		restricted.put(9, asSet(2, 3));

		runner = new ServletRunner(new File(getClass().getResource("WEB-INF/web.xml").toURI()), "/GCN");
	}

	/**
	 * Shut down the application
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		runner.shutDown();
	}

	/**
	 * Test making an SSO login request with a user, that is not restricted to nodes
	 * @throws Exception
	 */
	@Test
	public void testInitialUnrestrictedGroupSync() throws Exception {
		ServletUnitClient client = runner.newClient();
		makeSSORequest(client, UNRESTRICTED_NOSYNC_PATH, -1, unrestricted);
	}

	/**
	 * Test making an SSO login request with a user, that is restricted to nodes
	 * @throws Exception
	 */
	@Test
	public void testInitialRestrictedGroupSync() throws Exception {
		ServletUnitClient client = runner.newClient();
		makeSSORequest(client, RESTRICTED_NOSYNC_PATH, -1, restricted);
	}

	/**
	 * Test changing the node restrictions from unrestricted to restricted with an SSO login request
	 * @throws Exception
	 */
	@Test
	public void testRestrictingGroupSync() throws Exception {
		ServletUnitClient client = runner.newClient();
		int userId = makeSSORequest(client, UNRESTRICTED_SYNC_PATH, -1, unrestricted);
		makeSSORequest(client, RESTRICTED_SYNC_PATH, userId, restricted);
	}

	/**
	 * Test changing the node restrictions from restricted to unrestricted with an SSO login request
	 * @throws Exception
	 */
	@Test
	public void testUnrestrictingGroupSync() throws Exception {
		ServletUnitClient client = runner.newClient();
		int userId = makeSSORequest(client, RESTRICTED_SYNC_PATH, -1, restricted);
		makeSSORequest(client, UNRESTRICTED_SYNC_PATH, userId, unrestricted);
	}

	/**
	 * Test making an SSO request for an existing user without group sync
	 * @throws Exception
	 */
	@Test
	public void testRestrictingWithoutSync() throws Exception {
		ServletUnitClient client = runner.newClient();
		int userId = makeSSORequest(client, UNRESTRICTED_NOSYNC_PATH, -1, unrestricted);
		makeSSORequest(client, RESTRICTED_NOSYNC_PATH, userId, unrestricted);
	}

	/**
	 * Test changing the node restrictions from restricted to unrestricted with an SSO login request
	 * @throws Exception
	 */
	@Test
	public void testUnrestrictingWithoutSync() throws Exception {
		ServletUnitClient client = runner.newClient();
		int userId = makeSSORequest(client, RESTRICTED_NOSYNC_PATH, -1, restricted);
		makeSSORequest(client, UNRESTRICTED_NOSYNC_PATH, userId, restricted);
	}

	/**
	 * Test multithreaded access to the same login (must not create multiple users)
	 * @throws Exception
	 */
	@Test
	public void testMultithreadedSync() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

		List<Future<Boolean>> futures = new ArrayList<>();
		for (int i = 0; i < THREAD_COUNT; i++) {
			futures.add(executorService.submit(() -> {
				try {
					performRequest(runner.newClient(), UNRESTRICTED_NOSYNC_PATH, null, null);
					return true;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}));
		}

		for (Future<Boolean> future : futures) {
			future.get();
		}

		List<Integer> ids = new ArrayList<>();
		try (Trx trx = new Trx()) {
			DBUtils.executeStatement("SELECT id FROM systemuser WHERE login = ?", Transaction.SELECT_STATEMENT, st -> {
				st.setString(1, "testuser");
			}, rs -> {
				while (rs.next()) {
					ids.add(rs.getInt("id"));
				}
			});
			trx.success();
		}
		assertEquals("Duplicate users found ", 1, ids.size());
	}

	/**
	 * Make the SSO request and make some assertions
	 * @param client web client
	 * @param path path to which the request shall be done
	 * @param expectedUserId expected user id (if the call is made for an already existing user) or -1 if made for a new user
	 * @param expectedRestrictions expected node restrictions for the user (after the call)
	 * @return user id
	 * @throws Exception
	 */
	protected int makeSSORequest(ServletUnitClient client, String path, int expectedUserId, Map<Integer, Set<Integer>> expectedRestrictions) throws Exception {
		WebResponse response = performRequest(client, path, null, null);
		final int sid = ObjectTransformer.getInt(response.getText(), 0);
		assertTrue("Response '"+response.getText()+"' was no SID", sid != 0);

		testContext.getContext().startTransaction();
		final int[] userId = new int[1];
		DBUtils.executeStatement("SELECT user_id FROM systemsession WHERE id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, sid);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					userId[0] = rs.getInt("user_id");
				}
			}
		});

		if (expectedUserId > 0) {
			assertEquals("Check user id", expectedUserId, userId[0]);
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		SystemUser user = t.getObject(SystemUser.class, userId[0]);
		assertNotNull("User was not saved", user);

		assertRestrictions(expectedRestrictions, user.getGroupNodeRestrictions());

		return userId[0];
	}

	/**
	 * Perform a GET request to Portal.Node with the given request parameters and asserts the expected output (if any given)
	 * @param client client
	 * @param url url
	 * @param requestParameters map holding the request parameters
	 * @param expectedOutput expected output or null to not assert the output
	 * @return the response
	 * @throws Exception
	 */
	protected WebResponse performRequest(ServletUnitClient client, String url, Map<String, String> requestParameters, String expectedOutput) throws Exception {
	    WebRequest request = new GetMethodWebRequest( "http://test.meterware.com" + url );
	    request.setHeaderField("host", "testhost");
	    if (requestParameters != null) {
	    	for (Map.Entry<String, String> entry : requestParameters.entrySet()) {
				request.setParameter(entry.getKey(), entry.getValue());
			}
	    }
	    WebResponse response = client.getResponse( request );
	    assertNotNull( "No response received", response );
	    if (expectedOutput != null) {
	    	assertEquals( "requested resource", expectedOutput.replaceAll("\r\n", "\n"), response.getText().replaceAll("\r\n", "\n") );
	    }
	    return response;
	}

	/**
	 * Assert equality of the node restrictions
	 * @param expected expected node restrictions
	 * @param actual actual node restrictions
	 * @throws Exception
	 */
	protected void assertRestrictions(Map<Integer, Set<Integer>> expected, Map<Integer, Set<Integer>> actual) throws Exception {
		// check whether all expected groups are restricted
		assertSetEquals("Check restricted groups", expected.keySet(), actual.keySet());

		// check all groups
		for (Map.Entry<Integer, Set<Integer>> entry : expected.entrySet()) {
			int groupId = entry.getKey();
			Set<Integer> expectedNodeIds = entry.getValue();
			Set<Integer> actualNodeIds = actual.get(groupId);
			assertSetEquals("Check nodeIds for group " + groupId, expectedNodeIds, actualNodeIds);
		}
	}

	/**
	 * Assert equality of the given sets
	 * @param message message
	 * @param expected expected set
	 * @param actual actual set
	 * @throws Exception
	 */
	protected void assertSetEquals(String message, Set<Integer> expected, Set<Integer> actual) throws Exception {
		Set<Integer> diff = new HashSet<Integer>(expected);
		diff.removeAll(actual);
		assertTrue(message + ": Expected IDs " + diff + " where not found", diff.isEmpty());

		// check whether all restricted groups are expected to be restricted
		diff = new HashSet<Integer>(actual);
		diff.removeAll(expected);
		assertTrue(message + ": Unexpected IDs " + diff + " where found", diff.isEmpty());
	}

	/**
	 * Return a set containing the IDs of the given list of nodes
	 * @param nodeIds list of node IDs
	 * @return set containing the IDs of the nodes
	 */
	protected Set<Integer> asSet(Integer... nodeIds) {
		Set<Integer> set = new HashSet<Integer>();
		for (Integer nodeId : nodeIds) {
			set.add(nodeId);
		}
		return set;
	}
}
