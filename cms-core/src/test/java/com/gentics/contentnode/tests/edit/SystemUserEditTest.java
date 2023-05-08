package com.gentics.contentnode.tests.edit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test case for editing system users
 */
public class SystemUserEditTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Modified User
	 */
	private SystemUser modifiedUser;

	/**
	 * Unmodified USer
	 */
	private SystemUser unmodifiedUser;

	/**
	 * Tested group IDs
	 */
	private List<Integer> groupIds;

	/**
	 * Restricted node
	 */
	private Node restrictedNode;

	/**
	 * Unrestricted node
	 */
	private Node unrestrictedNode;

	@Before
	public void setUp() throws Exception {

		groupIds = new ArrayList<Integer>();
		DBUtils.executeStatement("SELECT id FROM usergroup WHERE id NOT IN (1, 2) LIMIT 3", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					groupIds.add(rs.getInt("id"));
				}
			}
		});
		assertEquals("Check # of groups to test", 3, groupIds.size());

		// create test data
		restrictedNode = ContentNodeTestDataUtils.createNode("Restricted", "restricted", "/", null, false, false);
		unrestrictedNode = ContentNodeTestDataUtils.createNode("Unrestricted", "unrestricted", "/", null, false, false);

		Map<Integer, Set<Integer>> expectedRestrictions = new HashMap<Integer, Set<Integer>>();
		expectedRestrictions.put(groupIds.get(0), asSet(restrictedNode));
		modifiedUser = createUser("Modified", "User", "modified");
		assertRestrictions(expectedRestrictions, modifiedUser.getGroupNodeRestrictions());
		unmodifiedUser = createUser("Unmodified", "User", "unmodified");
		assertRestrictions(expectedRestrictions, unmodifiedUser.getGroupNodeRestrictions());
	}

	/**
	 * Create a system user, assigned to the first two groups where the first group assignment is restricted to the node
	 * @param firstName first name
	 * @param lastName last name
	 * @param login login
	 * @return user
	 * @throws NodeException
	 */
	protected SystemUser createUser(String firstName, String lastName, String login) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		SystemUser user = t.createObject(SystemUser.class);
		user.setActive(true);
		user.setFirstname(firstName);
		user.setLastname(lastName);
		user.setEmail("");
		user.setLogin(login);
		user.setPassword("");
		user.getUserGroups().addAll(t.getObjects(UserGroup.class, groupIds.subList(0, 2)));
		user.getGroupNodeRestrictions().put(groupIds.get(0), asSet(restrictedNode));
		user.save();
		t.commit(false);

		return t.getObject(SystemUser.class, user.getId());
	}

	/**
	 * Test adding a node restriction to an already assigned group
	 * @throws Exception
	 */
	@Test
	public void testAddNodeRestrictions() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId(), true);
		modifiedUser.getGroupNodeRestrictions().put(groupIds.get(1), asSet(restrictedNode));
		modifiedUser.save();
		t.commit(false);
		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId());

		Map<Integer, Set<Integer>> expected = new HashMap<Integer, Set<Integer>>();
		expected.put(groupIds.get(0), asSet(restrictedNode));
		assertRestrictions(expected, unmodifiedUser.getGroupNodeRestrictions());

		expected.put(groupIds.get(1), asSet(restrictedNode));
		assertRestrictions(expected, modifiedUser.getGroupNodeRestrictions());
	}

	/**
	 * Test removing the node restriction to an already assigned group
	 * @throws Exception
	 */
	@Test
	public void testRemoveNodeRestrictions() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId(), true);
		modifiedUser.getGroupNodeRestrictions().remove(groupIds.get(0));
		modifiedUser.save();
		t.commit(false);
		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId());

		Map<Integer, Set<Integer>> expected = new HashMap<Integer, Set<Integer>>();
		expected.put(groupIds.get(0), asSet(restrictedNode));
		assertRestrictions(expected, unmodifiedUser.getGroupNodeRestrictions());

		expected.remove(groupIds.get(0));
		assertRestrictions(expected, modifiedUser.getGroupNodeRestrictions());
	}

	/**
	 * Test changing the node restriction to an assigned group (add restriction to another node)
	 * @throws Exception
	 */
	@Test
	public void testChangeNodeRestrictions() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId(), true);
		modifiedUser.getGroupNodeRestrictions().get(groupIds.get(0)).add(ObjectTransformer.getInt(unrestrictedNode.getId(), 0));
		modifiedUser.save();
		t.commit(false);
		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId());

		Map<Integer, Set<Integer>> expected = new HashMap<Integer, Set<Integer>>();
		expected.put(groupIds.get(0), asSet(restrictedNode));
		assertRestrictions(expected, unmodifiedUser.getGroupNodeRestrictions());

		expected.put(groupIds.get(0), asSet(restrictedNode, unrestrictedNode));
		assertRestrictions(expected, modifiedUser.getGroupNodeRestrictions());
	}

	/**
	 * Test assigning to a group (and restricting to a node)
	 * @throws Exception
	 */
	@Test
	public void testAddGroupWithRestrictions() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId(), true);
		modifiedUser.getUserGroups().add(t.getObject(UserGroup.class, groupIds.get(2)));
		modifiedUser.getGroupNodeRestrictions().put(groupIds.get(2), asSet(restrictedNode));
		modifiedUser.save();
		t.commit(false);
		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId());

		Map<Integer, Set<Integer>> expected = new HashMap<Integer, Set<Integer>>();
		expected.put(groupIds.get(0), asSet(restrictedNode));
		assertRestrictions(expected, unmodifiedUser.getGroupNodeRestrictions());

		expected.put(groupIds.get(2), asSet(restrictedNode));
		assertRestrictions(expected, modifiedUser.getGroupNodeRestrictions());
	}

	/**
	 * Test removing assignment to a group that was restricted to a node
	 * @throws Exception
	 */
	@Test
	public void testRemoveGroupWithRestrictions() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId(), true);
		modifiedUser.getUserGroups().remove(t.getObject(UserGroup.class, groupIds.get(0)));
		modifiedUser.save();
		t.commit(false);
		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId());

		Map<Integer, Set<Integer>> expected = new HashMap<Integer, Set<Integer>>();
		expected.put(groupIds.get(0), asSet(restrictedNode));
		assertRestrictions(expected, unmodifiedUser.getGroupNodeRestrictions());

		expected.remove(groupIds.get(0));
		assertRestrictions(expected, modifiedUser.getGroupNodeRestrictions());
	}

	/**
	 * Test restricting to a non existing node
	 * @throws Exception
	 */
	@Test
	public void testRestrictToNonexistingNode() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId(), true);
		modifiedUser.getGroupNodeRestrictions().get(groupIds.get(0)).add(999999);
		modifiedUser.getGroupNodeRestrictions().put(groupIds.get(1), new HashSet<Integer>(Arrays.asList(999999)));
		modifiedUser.save();
		t.commit(false);
		modifiedUser = t.getObject(SystemUser.class, modifiedUser.getId());

		Map<Integer, Set<Integer>> expected = new HashMap<Integer, Set<Integer>>();
		expected.put(groupIds.get(0), asSet(restrictedNode));
		assertRestrictions(expected, unmodifiedUser.getGroupNodeRestrictions());

		assertRestrictions(expected, modifiedUser.getGroupNodeRestrictions());
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
	 * @param nodes list of nodes
	 * @return set containing the IDs of the nodes
	 */
	protected Set<Integer> asSet(Node... nodes) {
		Set<Integer> set = new HashSet<Integer>();
		for (Node node : nodes) {
			set.add(ObjectTransformer.getInteger(node.getId(), null));
		}
		return set;
	}
}
