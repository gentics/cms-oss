package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.PERM_NODE_CONSTRUCT_MODIFY;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.model.PortalImplementationListResponse;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.PortalImplementationModel.Type;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.PortalImplementationResponse;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

/**
 * Test cases for assigning Portal Implementations to nodes via the REST resource /node/{nodeId}/portalimplementations
 */
public class NodePortalImplementationsTest {
	private static DBTestContext testContext = new DBTestContext();

	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	/**
	 * Admin user (member of the node group)
	 */
	private final static String ADMIN_LOGIN = "pinodeadmin";

	/**
	 * Restricted user (member of a subgroup of the node group)
	 */
	private final static String USER_LOGIN = "pinodeuser";

	private final static String PASSWORD = "test";

	/**
	 * Counter for unique keywords and names
	 */
	private static AtomicInteger counter = new AtomicInteger();

	private static UserGroup nodeGroup;

	private static UserGroup subGroup;

	private static Node node;

	/**
	 * Portal Implementation used in the tests
	 */
	private PortalImplementationModel impl;

	/**
	 * Body of the last response to a request changing an assignment (for assertion messages)
	 */
	private String lastResponseBody;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		if (TransactionManager.getCurrentTransactionOrNull() != null) {
			TransactionManager.getCurrentTransaction().commit();
		}

		nodeGroup = supply(t -> t.getObject(UserGroup.class, 2));
		subGroup = supply(() -> Creator.createUsergroup("PI Node Subgroup", "", nodeGroup));
		supply(() -> Creator.createUser(ADMIN_LOGIN, PASSWORD, "PI", "Admin", "", Arrays.asList(nodeGroup)));
		supply(() -> Creator.createUser(USER_LOGIN, PASSWORD, "PI", "User", "", Arrays.asList(subGroup)));

		node = supply(() -> createNode());

		operate(() -> {
			PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, Arrays.asList(nodeGroup), PermHandler.FULL_PERM);
			// the restricted group may access the admin parts and view portal implementations
			PermHandler.setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(subGroup), Permissions.get(PERM_VIEW).toString());
			PermHandler.setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(subGroup), Permissions.get(PERM_VIEW).toString());
			PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, Arrays.asList(subGroup), Permissions.get(PERM_VIEW).toString());
			PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(nodeGroup), PermHandler.FULL_PERM);
		});
	}

	@Before
	public void setup() throws Exception {
		operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (PortalImplementation existing : t.getObjects(PortalImplementation.class,
					DBUtils.select("SELECT id FROM portal_implementation", DBUtils.IDS))) {
				existing.delete(true);
			}
		});

		impl = create(ADMIN_LOGIN);

		// by default, the restricted user may view the node and the portal implementation, but may not change the assignments
		setNodePerm(PERM_VIEW);
		setInstancePerm(impl, PERM_VIEW);
	}

	/**
	 * Test assigning, listing and removing
	 * @throws Exception
	 */
	@Test
	public void testAssignListRemove() throws Exception {
		assertThat(assignedKeywords(node)).as("Assigned before").isEmpty();

		assertStatus(assign(ADMIN_LOGIN, node, Integer.toString(impl.getId())), Status.OK);
		assertThat(assignedKeywords(node)).as("Assigned after assigning").containsExactly(impl.getKeyword());
		assertThat(countAssignments()).as("Number of assignments").isEqualTo(1);

		// assigning again does not change anything
		assertStatus(assign(ADMIN_LOGIN, node, Integer.toString(impl.getId())), Status.OK);
		assertThat(countAssignments()).as("Number of assignments after assigning again").isEqualTo(1);

		assertStatus(unassign(ADMIN_LOGIN, node, Integer.toString(impl.getId())), Status.OK);
		assertThat(assignedKeywords(node)).as("Assigned after removing").isEmpty();
	}

	/**
	 * Test assigning by keyword and global ID, and assigning multiple Portal Implementations to multiple nodes
	 * @throws Exception
	 */
	@Test
	public void testAssignByKeywordAndGlobalId() throws Exception {
		PortalImplementationModel other = create(ADMIN_LOGIN);
		Node otherNode = supply(() -> createNode());
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, otherNode.getFolder().getId(), Arrays.asList(nodeGroup), PermHandler.FULL_PERM));

		assertStatus(assign(ADMIN_LOGIN, node, impl.getKeyword()), Status.OK);
		assertStatus(assign(ADMIN_LOGIN, node, other.getGlobalId()), Status.OK);
		assertStatus(assign(ADMIN_LOGIN, otherNode, impl.getKeyword()), Status.OK);

		assertThat(assignedKeywords(node)).as("Assigned to node").containsOnly(impl.getKeyword(), other.getKeyword());
		assertThat(assignedKeywords(otherNode)).as("Assigned to other node").containsOnly(impl.getKeyword());

		assertStatus(unassign(ADMIN_LOGIN, node, other.getGlobalId()), Status.OK);
		assertThat(assignedKeywords(node)).as("Assigned to node after removing").containsOnly(impl.getKeyword());
		assertThat(assignedKeywords(otherNode)).as("Assigned to other node after removing").containsOnly(impl.getKeyword());
	}

	/**
	 * Test permissions for changing assignments
	 * @throws Exception
	 */
	@Test
	public void testPermissions() throws Exception {
		// second portal implementation, the restricted user is not allowed to view
		PortalImplementationModel invisible = create(ADMIN_LOGIN);

		// without permission to update the constructs of the node
		assertStatus(assign(USER_LOGIN, node, impl.getKeyword()), Status.FORBIDDEN);
		assertThat(countAssignments()).as("Number of assignments").isEqualTo(0);

		// with permission on the node
		setNodePerm(PERM_VIEW, PERM_NODE_CONSTRUCT_MODIFY);

		// without permission to view the portal implementation
		assertStatus(assign(USER_LOGIN, node, invisible.getKeyword()), Status.FORBIDDEN);
		assertThat(countAssignments()).as("Number of assignments").isEqualTo(0);

		// with all necessary permissions
		assertStatus(assign(USER_LOGIN, node, impl.getKeyword()), Status.OK);
		assertThat(countAssignments()).as("Number of assignments").isEqualTo(1);

		// portal implementations, the user may not view, are not listed
		assertStatus(assign(ADMIN_LOGIN, node, invisible.getKeyword()), Status.OK);
		assertThat(assignedKeywords(USER_LOGIN, node)).as("Listed for restricted user").containsExactly(impl.getKeyword());
		assertThat(assignedKeywords(node)).as("Listed for admin").containsOnly(impl.getKeyword(), invisible.getKeyword());

		// removing requires the permission on the node
		setNodePerm(PERM_VIEW);
		assertStatus(unassign(USER_LOGIN, node, impl.getKeyword()), Status.FORBIDDEN);
		assertThat(countAssignments()).as("Number of assignments").isEqualTo(2);
	}

	/**
	 * Test assigning an unknown Portal Implementation
	 * @throws Exception
	 */
	@Test
	public void testUnknown() throws Exception {
		assertStatus(assign(ADMIN_LOGIN, node, "doesnotexist"), Status.NOT_FOUND);
		assertStatus(unassign(ADMIN_LOGIN, node, "doesnotexist"), Status.NOT_FOUND);
	}

	/**
	 * Test that deleting the Portal Implementation removes the assignments
	 * @throws Exception
	 */
	@Test
	public void testDeletePortalImplementation() throws Exception {
		assertStatus(assign(ADMIN_LOGIN, node, impl.getKeyword()), Status.OK);
		assertThat(countAssignments()).as("Number of assignments").isEqualTo(1);

		operate(t -> t.getObject(PortalImplementation.class, impl.getId()).delete(true));

		assertThat(countAssignments()).as("Number of assignments after deleting the Portal Implementation").isEqualTo(0);
	}

	/**
	 * Test that deleting the node removes the assignments
	 * @throws Exception
	 */
	@Test
	public void testDeleteNode() throws Exception {
		Node otherNode = supply(() -> createNode());
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, otherNode.getFolder().getId(), Arrays.asList(nodeGroup), PermHandler.FULL_PERM));
		assertStatus(assign(ADMIN_LOGIN, otherNode, impl.getKeyword()), Status.OK);
		assertStatus(assign(ADMIN_LOGIN, node, impl.getKeyword()), Status.OK);
		assertThat(countAssignments()).as("Number of assignments").isEqualTo(2);

		operate(t -> t.getObject(otherNode, true).delete(true));

		assertThat(supply(() -> DBUtils.select("SELECT node_id id FROM node_portal_implementation", DBUtils.IDS)))
				.as("Node IDs of assignments after deleting the node").containsOnly(node.getId());
	}

	/**
	 * Test listing the nodes, a Portal Implementation is assigned to
	 * @throws Exception
	 */
	@Test
	public void testListNodes() throws Exception {
		Node otherNode = supply(() -> createNode());
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, otherNode.getFolder().getId(), Arrays.asList(nodeGroup), PermHandler.FULL_PERM));
		Node unassignedNode = supply(() -> createNode());
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, unassignedNode.getFolder().getId(), Arrays.asList(nodeGroup), PermHandler.FULL_PERM));

		assertThat(nodeIds(ADMIN_LOGIN, impl)).as("Nodes before assignment").isEmpty();

		assertStatus(assign(ADMIN_LOGIN, node, impl.getKeyword()), Status.OK);
		assertStatus(assign(ADMIN_LOGIN, otherNode, impl.getKeyword()), Status.OK);

		assertThat(nodeIds(ADMIN_LOGIN, impl)).as("Nodes for admin").containsOnly(node.getId(), otherNode.getId());

		// the restricted user may only view the first node
		assertThat(nodeIds(USER_LOGIN, impl)).as("Nodes for restricted user").containsOnly(node.getId());

		// the restricted user may not view this portal implementation
		PortalImplementationModel invisible = create(ADMIN_LOGIN);
		try (LoggedInClient client = restContext.client(USER_LOGIN, PASSWORD)) {
			Response response = client.get().base().path("portalimplementations").path(Integer.toString(invisible.getId())).path("nodes").request().get();
			assertThat(response.getStatus()).as("Response status for Portal Implementation without permission").isEqualTo(Status.FORBIDDEN.getStatusCode());
			response.close();
		}
	}

	/**
	 * Get the IDs of the nodes, the Portal Implementation is assigned to, via GET /portalimplementations/{id}/nodes
	 * @param login login
	 * @param impl Portal Implementation
	 * @return list of node IDs
	 * @throws Exception
	 */
	protected List<Integer> nodeIds(String login, PortalImplementationModel impl) throws Exception {
		try (LoggedInClient client = restContext.client(login, PASSWORD)) {
			NodeList response = client.get().base().path("portalimplementations").path(Integer.toString(impl.getId())).path("nodes").request()
					.get(NodeList.class);
			client.get().assertResponse(response);
			return response.getItems().stream().map(com.gentics.contentnode.rest.model.Node::getId).collect(Collectors.toList());
		}
	}

	/**
	 * Create a Portal Implementation via REST
	 * @param login login
	 * @return created Portal Implementation
	 * @throws Exception
	 */
	protected PortalImplementationModel create(String login) throws Exception {
		int index = counter.incrementAndGet();
		try (LoggedInClient client = restContext.client(login, PASSWORD)) {
			PortalImplementationResponse response = client.get().base().path("portalimplementations").request()
					.post(Entity.entity(new PortalImplementationModel().setKeyword("portal" + index).setName("Portal " + index).setType(Type.PHP),
							MediaType.APPLICATION_JSON_TYPE), PortalImplementationResponse.class);
			client.get().assertResponse(response);
			return response.getPortalImplementation();
		}
	}

	/**
	 * Assign the Portal Implementation to the node
	 * @param login login
	 * @param node node
	 * @param portalImplementation Portal Implementation ID
	 * @return response status
	 * @throws Exception
	 */
	protected int assign(String login, Node node, String portalImplementation) throws Exception {
		try (LoggedInClient client = restContext.client(login, PASSWORD)) {
			Response response = client.get().base().path("node").path(Integer.toString(node.getId())).path("portalimplementations")
					.path(portalImplementation).request().put(Entity.json(""));
			int status = response.getStatus();
			lastResponseBody = response.readEntity(String.class);
			response.close();
			return status;
		}
	}

	/**
	 * Remove the Portal Implementation from the node
	 * @param login login
	 * @param node node
	 * @param portalImplementation Portal Implementation ID
	 * @return response status
	 * @throws Exception
	 */
	protected int unassign(String login, Node node, String portalImplementation) throws Exception {
		try (LoggedInClient client = restContext.client(login, PASSWORD)) {
			Response response = client.get().base().path("node").path(Integer.toString(node.getId())).path("portalimplementations")
					.path(portalImplementation).request().delete();
			int status = response.getStatus();
			lastResponseBody = response.readEntity(String.class);
			response.close();
			return status;
		}
	}

	/**
	 * Get the keywords of the Portal Implementations assigned to the node (as admin)
	 * @param node node
	 * @return list of keywords
	 * @throws Exception
	 */
	protected List<String> assignedKeywords(Node node) throws Exception {
		return assignedKeywords(ADMIN_LOGIN, node);
	}

	/**
	 * Get the keywords of the Portal Implementations assigned to the node
	 * @param login login
	 * @param node node
	 * @return list of keywords
	 * @throws Exception
	 */
	protected List<String> assignedKeywords(String login, Node node) throws Exception {
		try (LoggedInClient client = restContext.client(login, PASSWORD)) {
			PortalImplementationListResponse response = client.get().base().path("node").path(Integer.toString(node.getId())).path("portalimplementations")
					.request().get(PortalImplementationListResponse.class);
			client.get().assertResponse(response);
			return response.getItems().stream().map(PortalImplementationModel::getKeyword).collect(Collectors.toList());
		}
	}

	/**
	 * Count the assignments
	 * @return number of assignments
	 * @throws NodeException
	 */
	protected int countAssignments() throws NodeException {
		return supply(() -> DBUtils.select("SELECT COUNT(*) c FROM node_portal_implementation", rs -> rs.next() ? rs.getInt("c") : 0));
	}

	/**
	 * Assert the response status
	 * @param status actual status
	 * @param expected expected status
	 */
	protected void assertStatus(int status, Status expected) {
		assertThat(status).as("Response status (response: %s)", lastResponseBody).isEqualTo(expected.getStatusCode());
	}

	/**
	 * Set the permissions of the restricted user's group on the node
	 * @param bits permission bits
	 * @throws NodeException
	 */
	protected void setNodePerm(int... bits) throws NodeException {
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(subGroup), permString(bits)));
	}

	/**
	 * Set the permissions of the restricted user's group on the Portal Implementation
	 * @param impl Portal Implementation
	 * @param bits permission bits
	 * @throws NodeException
	 */
	protected void setInstancePerm(PortalImplementationModel impl, int... bits) throws NodeException {
		operate(() -> PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATION, impl.getId(), Arrays.asList(subGroup),
				permString(bits)));
	}

	/**
	 * Get the permission string for the given bits
	 * @param bits permission bits (may be empty)
	 * @return permission string
	 * @throws NodeException
	 */
	protected static String permString(int... bits) throws NodeException {
		return bits.length == 0 ? PermHandler.EMPTY_PERM : Permissions.get(bits).toString();
	}
}
