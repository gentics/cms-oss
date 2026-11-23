package com.gentics.contentnode.tests.rest.node;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.PortalType;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for setting and loading the portal type of nodes over the REST API
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY })
public class NodePortalTypeRestTest {
	protected final static String PROPERTY_NAME = "NODE_PORTALTYPE_REST";

	protected final static String PROPERTY = "${sys:" + PROPERTY_NAME + "}";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@After
	public void cleanup() throws NodeException {
		System.clearProperty(PROPERTY_NAME);
		Trx.operate(tx -> {
			for (com.gentics.contentnode.object.Node node : tx.getObjects(com.gentics.contentnode.object.Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS), true)) {
				node.delete(true);
			}
		});
	}

	/**
	 * Test creating a node with a portal type
	 * @throws NodeException
	 */
	@Test
	public void testCreateWithValue() throws NodeException {
		Node request = newNode();
		request.setPortalType(PortalType.Java);

		NodeLoadResponse created = create(request);
		assertThat(created.getNode().getPortalType()).as("Portal type in create response").isEqualTo(PortalType.Java);

		Node loaded = load(created.getNode().getId());
		assertThat(loaded.getPortalType()).as("Loaded portal type").isEqualTo(PortalType.Java);
		assertThat(loaded.getPortalTypeProperty()).as("Loaded portal type property").isEmpty();
	}

	/**
	 * Test creating a node without portal type, which is allowed over the REST API
	 * @throws NodeException
	 */
	@Test
	public void testCreateWithoutValue() throws NodeException {
		NodeLoadResponse created = create(newNode());

		Node loaded = load(created.getNode().getId());
		assertThat(loaded.getPortalType()).as("Loaded portal type").isNull();
	}

	/**
	 * Test creating a node with a portal type property
	 * @throws NodeException
	 */
	@Test
	public void testCreateWithProperty() throws NodeException {
		System.setProperty(PROPERTY_NAME, "php");

		Node request = newNode();
		request.setPortalTypeProperty(PROPERTY);

		NodeLoadResponse created = create(request);

		Node loaded = load(created.getNode().getId());
		assertThat(loaded.getPortalTypeProperty()).as("Loaded portal type property").isEqualTo(PROPERTY);
		assertThat(loaded.getPortalType()).as("Loaded (resolved) portal type").isEqualTo(PortalType.PHP);
	}

	/**
	 * Test changing the portal type of a node, first directly, then with a property and then directly again
	 * @throws NodeException
	 */
	@Test
	public void testUpdate() throws NodeException {
		Node createRequest = newNode();
		createRequest.setPortalType(PortalType.Java);
		int id = create(createRequest).getNode().getId();

		Node update = new Node();
		update.setPortalType(PortalType.PHP);
		update(id, update);
		assertThat(load(id).getPortalType()).as("Portal type after updating the value").isEqualTo(PortalType.PHP);

		System.setProperty(PROPERTY_NAME, "Java");
		update = new Node();
		update.setPortalTypeProperty(PROPERTY);
		update(id, update);
		assertThat(load(id).getPortalType()).as("Portal type after setting the property").isEqualTo(PortalType.Java);

		// this is what the UI sends, when switching back from the property to the value
		update = new Node();
		update.setPortalType(PortalType.PHP);
		update.setPortalTypeProperty("");
		update(id, update);
		Node loaded = load(id);
		assertThat(loaded.getPortalType()).as("Portal type after removing the property").isEqualTo(PortalType.PHP);
		assertThat(loaded.getPortalTypeProperty()).as("Portal type property after removing it").isEmpty();
	}

	protected Node newNode() {
		Node node = new Node();
		node.setName("Portal Type Node");
		node.setHost("portaltype.node.com");
		node.setUtf8(true);
		return node;
	}

	protected NodeLoadResponse create(Node node) throws NodeException {
		NodeLoadResponse response = Trx.supply(t -> {
			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);
			return new NodeResourceImpl().add(request);
		});
		ContentNodeRESTUtils.assertResponseOK(response);
		return response;
	}

	protected void update(int id, Node node) throws NodeException {
		GenericResponse response = Trx.supply(t -> {
			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);
			return new NodeResourceImpl().update(Integer.toString(id), request);
		});
		ContentNodeRESTUtils.assertResponseOK(response);
	}

	protected Node load(int id) throws NodeException {
		NodeLoadResponse response = Trx.supply(t -> new NodeResourceImpl().get(Integer.toString(id), false));
		ContentNodeRESTUtils.assertResponseOK(response);
		return response.getNode();
	}
}
