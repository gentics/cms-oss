package com.gentics.contentnode.tests.auth;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.filter.HttpAuthFilter;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.servlet.filters.ModifyRequestFilter;

public class SSOUserSyncTest {
	protected static Path getResourcePath() {
		try {
			return Paths.get(SSOUserSyncTest.class.getResource("WEB-INF").toURI());
		} catch (URISyntaxException e) {
			fail("Failed to get path for resources", e);
			return null;
		}
	}

	@ClassRule
	public static DBTestContext testContext = new DBTestContext().config(map -> {
		map.setProperty("http_auth_login.login", "HTTP_LOGIN");
		map.setProperty("http_auth_login.pw", "HTTP_PW");
		map.setProperty("http_auth_login.firstname", "HTTP_FIRST");
		map.setProperty("http_auth_login.lastname", "HTTP_LAST");
		map.setProperty("http_auth_login.email", "HTTP_MAIL");
		map.setProperty("http_auth_login.group", "HTTP_GROUP");
	});

	@SuppressWarnings("unchecked")
	@ClassRule
	public static ServletAppContext context = new ServletAppContext()
		.baseResource(getResourcePath())
		.servlet("/test/*", TestServlet.class)
		.filter("/test/unrestricted/*", ModifyRequestFilter.class, Pair.of("headers", "/auth_headers_unrestricted.properties"))
		.filter("/test/restricted/*", ModifyRequestFilter.class, Pair.of("headers", "/auth_headers_restricted.properties"))
		.filter("/test/unrestricted/nosync", HttpAuthFilter.class)
		.filter("/test/restricted/nosync", HttpAuthFilter.class)
		.filter("/test/unrestricted/sync", HttpAuthFilter.class, Pair.of("syncGroups", "true"))
		.filter("/test/restricted/sync", HttpAuthFilter.class, Pair.of("syncGroups", "true"));

	/**
	 * Path using the unrestricted filter without group sync for existing users
	 */
	public final static String UNRESTRICTED_NOSYNC_PATH = "test/unrestricted/nosync";

	/**
	 * Path using the unrestricted filter with group sync for existing users
	 */
	public final static String UNRESTRICTED_SYNC_PATH = "test/unrestricted/sync";

	/**
	 * Path using the restricted filter without group sync for existing users
	 */
	public final static String RESTRICTED_NOSYNC_PATH = "test/restricted/nosync";

	/**
	 * Path using the restricted filter with group sync for existing users
	 */
	public final static String RESTRICTED_SYNC_PATH = "test/restricted/sync";

	/**
	 * Thread count for the multithreaded test
	 */
	private static final int THREAD_COUNT = 20;

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
	}

	/**
	 * Test making an SSO login request with a user, that is not restricted to nodes
	 * @throws Exception
	 */
	@Test
	public void testInitialUnrestrictedGroupSync() throws Exception {
		makeSSORequest(new HttpClient(), UNRESTRICTED_NOSYNC_PATH, -1, unrestricted);
	}

	/**
	 * Test making an SSO login request with a user, that is restricted to nodes
	 * @throws Exception
	 */
	@Test
	public void testInitialRestrictedGroupSync() throws Exception {
		makeSSORequest(new HttpClient(), RESTRICTED_NOSYNC_PATH, -1, restricted);
	}

	/**
	 * Test changing the node restrictions from unrestricted to restricted with an SSO login request
	 * @throws Exception
	 */
	@Test
	public void testRestrictingGroupSync() throws Exception {
		HttpClient client = new HttpClient();
		int userId = makeSSORequest(client, UNRESTRICTED_SYNC_PATH, -1, unrestricted);
		makeSSORequest(client, RESTRICTED_SYNC_PATH, userId, restricted);
	}

	/**
	 * Test changing the node restrictions from restricted to unrestricted with an SSO login request
	 * @throws Exception
	 */
	@Test
	public void testUnrestrictingGroupSync() throws Exception {
		HttpClient client = new HttpClient();
		int userId = makeSSORequest(client, RESTRICTED_SYNC_PATH, -1, restricted);
		makeSSORequest(client, UNRESTRICTED_SYNC_PATH, userId, unrestricted);
	}

	/**
	 * Test making an SSO request for an existing user without group sync
	 * @throws Exception
	 */
	@Test
	public void testRestrictingWithoutSync() throws Exception {
		HttpClient client = new HttpClient();
		int userId = makeSSORequest(client, UNRESTRICTED_NOSYNC_PATH, -1, unrestricted);
		makeSSORequest(client, RESTRICTED_NOSYNC_PATH, userId, unrestricted);
	}

	/**
	 * Test changing the node restrictions from restricted to unrestricted with an SSO login request
	 * @throws Exception
	 */
	@Test
	public void testUnrestrictingWithoutSync() throws Exception {
		HttpClient client = new HttpClient();
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
					performRequest(new HttpClient(), UNRESTRICTED_NOSYNC_PATH);
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
	protected int makeSSORequest(HttpClient client, String path, int expectedUserId, Map<Integer, Set<Integer>> expectedRestrictions) throws Exception {
		String response = performRequest(client, path);
		final int sid = ObjectTransformer.getInt(response, 0);
		assertTrue("Response '"+response+"' was no SID", sid != 0);

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
	 * @return the response
	 * @throws Exception
	 */
	protected String performRequest(HttpClient client, String url) throws Exception {
		GetMethod getMethod = new GetMethod(context.getBaseUri() + url);
		getMethod.addRequestHeader("host", "testhost");
		client.executeMethod(getMethod);
		return getMethod.getResponseBodyAsString();
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
