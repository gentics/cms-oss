package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for manipulating hostnames/http flags of a node
 */
public class NodeHostnameTest {

	protected final static String PROPERTY_NODE = "NODE_HOST_PORTAL";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@After
	public void cleanup() throws NodeException {
		System.clearProperty(PROPERTY_NODE);
		Trx.operate(tx -> {
			for (com.gentics.contentnode.object.Node node : tx.getObjects(com.gentics.contentnode.object.Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS), true)) {
				node.delete(true);
			}
		});
	}

	@Test
	public void testCreateForceInsecure() throws NodeException {
		NodeLoadResponse response = Trx.supply(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name insecure");
			node.setHost("http://nodeinsecure.com");
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			return resource.add(request);
		});

		ContentNodeRESTUtils.assertResponseOK(response);

		Trx.operate(t -> {
			com.gentics.contentnode.object.Node node = t.getObject(com.gentics.contentnode.object.Node.class, response.getNode().getId());
			assertThat(node).hasName("Node name insecure").hasHostname("nodeinsecure.com").isHttp();
		});
	}

	@Test
	public void testCreateForceSecure() throws NodeException {
		NodeLoadResponse response = Trx.supply(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name secure");
			node.setHost("https://nodesecure.com");
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			return resource.add(request);
		});

		ContentNodeRESTUtils.assertResponseOK(response);

		Trx.operate(t -> {
			com.gentics.contentnode.object.Node node = t.getObject(com.gentics.contentnode.object.Node.class, response.getNode().getId());
			assertThat(node).hasName("Node name secure").hasHostname("nodesecure.com").isHttps();
		});
	}

	@Test
	public void testCreateWithPropertyForceInsecure() throws NodeException {
		System.setProperty(PROPERTY_NODE, "http://nodeinsecure.com");

		NodeLoadResponse response = Trx.supply(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name insecure");
			node.setHostProperty("${sys:" + PROPERTY_NODE + "}");
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			return resource.add(request);
		});

		ContentNodeRESTUtils.assertResponseOK(response);

		Trx.operate(t -> {
			com.gentics.contentnode.object.Node node = t.getObject(com.gentics.contentnode.object.Node.class, response.getNode().getId());
			assertThat(node).hasName("Node name insecure").hasHostname("nodeinsecure.com").isHttp();
		});
	}

	@Test
	public void testCreateWithPropertyForceSecure() throws NodeException {
		System.setProperty(PROPERTY_NODE, "https://nodesecure.com");

		NodeLoadResponse response = Trx.supply(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name secure");
			node.setHostProperty("${sys:" + PROPERTY_NODE + "}");
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			return resource.add(request);
		});

		ContentNodeRESTUtils.assertResponseOK(response);

		Trx.operate(t -> {
			com.gentics.contentnode.object.Node node = t.getObject(com.gentics.contentnode.object.Node.class, response.getNode().getId());
			assertThat(node).hasName("Node name secure").hasHostname("nodesecure.com").isHttps();
		});
	}

	/**
	 * Test creating a node with "pub dir segments"
	 * @throws NodeException
	 */
	@Test
	public void testCreateSegmentExists() throws NodeException {
		int id = makeNode();

		Trx.operate(t -> {
			com.gentics.contentnode.object.Node node = t.getObject(com.gentics.contentnode.object.Node.class, id);
			assertThat(node).hasName("Node name").hasHostname("node.com").isHttp();
		});

		NodeLoadResponse response = Trx.supply(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name existing");
			node.setHost("https://node.com");
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			return resource.add(request);
		});

		ContentNodeRESTUtils.assertResponse(response, ResponseCode.INVALIDDATA);
	}

	@Test
	public void testUpdateForceInsecure() throws NodeException {
		int id = makeNode();

		GenericResponse response = Trx.supply(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name insecure");
			node.setHost("http://nodeinsecure.com");
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			return resource.update(Integer.toString(id), request);
		});

		ContentNodeRESTUtils.assertResponseOK(response);

		Trx.operate(t -> {
			com.gentics.contentnode.object.Node node = t.getObject(com.gentics.contentnode.object.Node.class, id);
			assertThat(node).hasName("Node name insecure").hasHostname("nodeinsecure.com").isHttp();
		});
	}

	@Test
	public void testUpdateForceSecure() throws NodeException {
		int id = makeNode();

		GenericResponse response = Trx.supply(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name secure");
			node.setHost("https://nodesecure.com");
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			return resource.update(Integer.toString(id), request);
		});

		ContentNodeRESTUtils.assertResponseOK(response);

		Trx.operate(t -> {
			com.gentics.contentnode.object.Node node = t.getObject(com.gentics.contentnode.object.Node.class, id);
			assertThat(node).hasName("Node name secure").hasHostname("nodesecure.com").isHttps();
		});
	}

	@Test
	public void testUpdateWithPropertyForceInsecure() throws NodeException {
		System.setProperty(PROPERTY_NODE, "http://nodeinsecure.com");

		int id = makeNode();

		GenericResponse response = Trx.supply(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name insecure");
			node.setHostProperty("${sys:" + PROPERTY_NODE + "}");
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			return resource.update(Integer.toString(id), request);
		});

		ContentNodeRESTUtils.assertResponseOK(response);

		Trx.operate(t -> {
			com.gentics.contentnode.object.Node node = t.getObject(com.gentics.contentnode.object.Node.class, id);
			assertThat(node).hasName("Node name insecure").hasHostname("nodeinsecure.com").isHttp();
		});
	}

	@Test
	public void testUpdateWithPropertyForceSecure() throws NodeException {
		System.setProperty(PROPERTY_NODE, "https://nodesecure.com");

		int id = makeNode();

		GenericResponse response = Trx.supply(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name secure");
			node.setHostProperty("${sys:" + PROPERTY_NODE + "}");
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			return resource.update(Integer.toString(id), request);
		});

		ContentNodeRESTUtils.assertResponseOK(response);

		Trx.operate(t -> {
			com.gentics.contentnode.object.Node node = t.getObject(com.gentics.contentnode.object.Node.class, id);
			assertThat(node).hasName("Node name secure").hasHostname("nodesecure.com").isHttps();
		});
	}

	public int makeNode() throws NodeException {
		NodeLoadResponse response = Trx.supply(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name");
			node.setHost("node.com");
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			return resource.add(request);
		});
		ContentNodeRESTUtils.assertResponseOK(response);

		return response.getNode().getId();
	}
}
