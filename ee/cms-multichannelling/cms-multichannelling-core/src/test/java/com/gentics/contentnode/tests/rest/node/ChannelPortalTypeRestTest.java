package com.gentics.contentnode.tests.rest.node;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for the portal type of channels, which are created over the REST API
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING })
public class ChannelPortalTypeRestTest {
	protected final static String PROPERTY_NAME = "NODE_PORTALTYPE_CHANNEL";

	protected final static String PROPERTY = "${sys:" + PROPERTY_NAME + "}";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@After
	public void cleanup() throws NodeException {
		System.clearProperty(PROPERTY_NAME);
		Trx.operate(tx -> {
			List<com.gentics.contentnode.object.Node> nodes = tx.getObjects(com.gentics.contentnode.object.Node.class,
					DBUtils.select("SELECT id FROM node", DBUtils.IDS), true);
			// channels must be deleted before their masters
			for (com.gentics.contentnode.object.Node node : nodes) {
				if (node.isChannel()) {
					node.delete(true);
				}
			}
			for (com.gentics.contentnode.object.Node node : nodes) {
				if (!node.isChannel()) {
					node.delete(true);
				}
			}
		});
	}

	/**
	 * Test that a channel created without portal type gets the portal type of the master
	 * @throws NodeException
	 */
	@Test
	public void testChannelGetsValueOfMaster() throws NodeException {
		Node master = newNode("master.portaltype.com");
		master.setPortalType(PortalType.PHP);
		Node createdMaster = create(master);

		Node channel = load(create(newChannel(createdMaster, "channel.portaltype.com")).getId());
		assertThat(channel.getPortalType()).as("Portal type of the channel").isEqualTo(PortalType.PHP);
		assertThat(channel.getPortalTypeProperty()).as("Portal type property of the channel").isEmpty();
	}

	/**
	 * Test that a channel created without portal type gets the portal type property of the master, which is resolved for the channel
	 * @throws NodeException
	 */
	@Test
	public void testChannelGetsPropertyOfMaster() throws NodeException {
		System.setProperty(PROPERTY_NAME, "php");

		Node master = newNode("master.portaltype.com");
		master.setPortalTypeProperty(PROPERTY);
		Node createdMaster = create(master);

		Node channel = load(create(newChannel(createdMaster, "channel.portaltype.com")).getId());
		assertThat(channel.getPortalTypeProperty()).as("Portal type property of the channel").isEqualTo(PROPERTY);
		assertThat(channel.getPortalType()).as("Resolved portal type of the channel").isEqualTo(PortalType.PHP);
	}

	/**
	 * Test that a channel created with a portal type keeps it, even if the master has another one
	 * @throws NodeException
	 */
	@Test
	public void testChannelKeepsOwnValue() throws NodeException {
		Node master = newNode("master.portaltype.com");
		master.setPortalType(PortalType.Java);
		Node createdMaster = create(master);

		Node channelRequest = newChannel(createdMaster, "channel.portaltype.com");
		channelRequest.setPortalType(PortalType.PHP);
		Node channel = load(create(channelRequest).getId());
		assertThat(channel.getPortalType()).as("Portal type of the channel").isEqualTo(PortalType.PHP);

		assertThat(load(createdMaster.getId()).getPortalType()).as("Portal type of the master").isEqualTo(PortalType.Java);
	}

	protected Node newNode(String host) {
		Node node = new Node();
		node.setName(host);
		node.setHost(host);
		node.setUtf8(true);
		return node;
	}

	protected Node newChannel(Node master, String host) {
		Node channel = newNode(host);
		channel.setMasterId(master.getFolderId());
		return channel;
	}

	protected Node create(Node node) throws NodeException {
		NodeLoadResponse response = Trx.supply(t -> {
			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);
			return new NodeResourceImpl().add(request);
		});
		ContentNodeRESTUtils.assertResponseOK(response);
		return response.getNode();
	}

	protected Node load(int id) throws NodeException {
		NodeLoadResponse response = Trx.supply(t -> new NodeResourceImpl().get(Integer.toString(id), false));
		ContentNodeRESTUtils.assertResponseOK(response);
		return response.getNode();
	}
}
