package com.gentics.contentnode.tests.rest.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for creation of nodes
 */
public class NodeCreationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test creating a node with "pub dir segments"
	 * @throws NodeException
	 */
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
			assertEquals(node.getName(), "Node name insecure");
			assertEquals(node.getHostname(), "nodeinsecure.com");
			assertFalse(node.isHttps());
		});
	}

	/**
	 * Test creating a node with "pub dir segments"
	 * @throws NodeException
	 */
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
			assertEquals(node.getName(), "Node name secure");
			assertEquals(node.getHostname(), "nodesecure.com");
			assertTrue(node.isHttps());
		});
	}

	/**
	 * Test creating a node with "pub dir segments"
	 * @throws NodeException
	 */
	@Test
	public void testCreateSegmentExists() throws NodeException {
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
		int id = response.getNode().getId();

		ContentNodeRESTUtils.assertResponseOK(response);

		Trx.operate(t -> {
			com.gentics.contentnode.object.Node node = t.getObject(com.gentics.contentnode.object.Node.class, id);
			assertEquals(node.getName(), "Node name");
			assertEquals(node.getHostname(), "node.com");
			assertFalse(node.isHttps());
		});

		response = Trx.supply(t -> {
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
}
