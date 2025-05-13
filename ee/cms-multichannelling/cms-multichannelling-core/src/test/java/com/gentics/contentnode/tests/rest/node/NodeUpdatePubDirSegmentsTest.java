package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getNodeResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.clearNodeObjectCache;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Condition;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for updating the publish directory segments flag for nodes with channels
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.PUB_DIR_SEGMENT })
public class NodeUpdatePubDirSegmentsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Condition which checks whether the nodes have the flag set
	 */
	private static Condition<com.gentics.contentnode.rest.model.Node> publishingDirSegment = new Condition<>(
			n -> n.isPubDirSegment(), "Publishing directory segments");

	/**
	 * Condition which checks whether the nodes have the flag cleared
	 */
	private static Condition<com.gentics.contentnode.rest.model.Node> notPublishingDirSegment = new Condition<>(
			n -> !n.isPubDirSegment(), "Not publishing directory segments");

	private static Node node;
	private static Node channel;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});

		node = supply(() -> createNode("node.com", "Node", PublishTarget.NONE));
		channel = supply(() -> createChannel(node, "Channel", "channel.com", "/"));
		supply(() -> createChannel(channel, "SubChannel", "sub.channel.com", "/"));
		supply(() -> createChannel(node, "SideChannel", "side.channel.com", "/"));
	}

	/**
	 * Test setting the flag for the master node
	 * @throws NodeException
	 * @throws PortalCacheException
	 */
	@Test
	public void testSetFlagOnMaster() throws NodeException, PortalCacheException {
		operate(() -> {
			DBUtils.update("UPDATE node SET pub_dir_segment = ?", false);
		});
		clearNodeObjectCache();

		NodeList nodeList = supply(() -> getNodeResource().list(null, null, null, null, null));
		assertResponseOK(nodeList);
		assertThat(nodeList.getItems()).as("Node List").are(notPublishingDirSegment);

		operate(() -> {
			String nodeId = Integer.toString(node.getId());
			NodeLoadResponse loadResponse = getNodeResource().get(nodeId, false);
			assertResponseOK(loadResponse);
			com.gentics.contentnode.rest.model.Node n = loadResponse.getNode();
			n.setPubDirSegment(true);

			NodeSaveRequest save = new NodeSaveRequest();
			save.setNode(n);
			GenericResponse saveResponse = getNodeResource().update(nodeId, save);
			assertResponseOK(saveResponse);
		});

		nodeList = supply(() -> getNodeResource().list(null, null, null, null, null));
		assertResponseOK(nodeList);
		assertThat(nodeList.getItems()).as("Node List").are(publishingDirSegment);
	}

	/**
	 * Test clearing the flag for the master node
	 * @throws NodeException
	 * @throws PortalCacheException
	 */
	@Test
	public void testClearFlagOnMaster() throws NodeException, PortalCacheException {
		operate(() -> {
			DBUtils.update("UPDATE node SET pub_dir_segment = ?", true);
		});
		clearNodeObjectCache();

		NodeList nodeList = supply(() -> getNodeResource().list(null, null, null, null, null));
		assertResponseOK(nodeList);
		assertThat(nodeList.getItems()).as("Node List").are(publishingDirSegment);

		operate(() -> {
			String nodeId = Integer.toString(node.getId());
			NodeLoadResponse loadResponse = getNodeResource().get(nodeId, false);
			assertResponseOK(loadResponse);
			com.gentics.contentnode.rest.model.Node n = loadResponse.getNode();
			n.setPubDirSegment(false);

			NodeSaveRequest save = new NodeSaveRequest();
			save.setNode(n);
			GenericResponse saveResponse = getNodeResource().update(nodeId, save);
			assertResponseOK(saveResponse);
		});

		nodeList = supply(() -> getNodeResource().list(null, null, null, null, null));
		assertResponseOK(nodeList);
		assertThat(nodeList.getItems()).as("Node List").are(notPublishingDirSegment);
	}

	/**
	 * Test setting the flag for a channel node
	 * @throws NodeException
	 * @throws PortalCacheException
	 */
	@Test
	public void testSetFlagOnChannel() throws NodeException, PortalCacheException {
		operate(() -> {
			DBUtils.update("UPDATE node SET pub_dir_segment = ?", false);
		});
		clearNodeObjectCache();

		NodeList nodeList = supply(() -> getNodeResource().list(null, null, null, null, null));
		assertResponseOK(nodeList);
		assertThat(nodeList.getItems()).as("Node List").are(notPublishingDirSegment);

		operate(() -> {
			String nodeId = Integer.toString(channel.getId());
			NodeLoadResponse loadResponse = getNodeResource().get(nodeId, false);
			assertResponseOK(loadResponse);
			com.gentics.contentnode.rest.model.Node n = loadResponse.getNode();
			n.setPubDirSegment(true);

			NodeSaveRequest save = new NodeSaveRequest();
			save.setNode(n);
			GenericResponse saveResponse = getNodeResource().update(nodeId, save);
			assertResponseOK(saveResponse);
		});

		nodeList = supply(() -> getNodeResource().list(null, null, null, null, null));
		assertResponseOK(nodeList);
		assertThat(nodeList.getItems()).as("Node List").are(notPublishingDirSegment);
	}

	/**
	 * Test clearing the flag for a channel node
	 * @throws NodeException
	 * @throws PortalCacheException
	 */
	@Test
	public void testClearFlagOnChannel() throws NodeException, PortalCacheException {
		operate(() -> {
			DBUtils.update("UPDATE node SET pub_dir_segment = ?", true);
		});
		clearNodeObjectCache();

		NodeList nodeList = supply(() -> getNodeResource().list(null, null, null, null, null));
		assertResponseOK(nodeList);
		assertThat(nodeList.getItems()).as("Node List").are(publishingDirSegment);

		operate(() -> {
			String nodeId = Integer.toString(channel.getId());
			NodeLoadResponse loadResponse = getNodeResource().get(nodeId, false);
			assertResponseOK(loadResponse);
			com.gentics.contentnode.rest.model.Node n = loadResponse.getNode();
			n.setPubDirSegment(false);

			NodeSaveRequest save = new NodeSaveRequest();
			save.setNode(n);
			GenericResponse saveResponse = getNodeResource().update(nodeId, save);
			assertResponseOK(saveResponse);
		});

		nodeList = supply(() -> getNodeResource().list(null, null, null, null, null));
		assertResponseOK(nodeList);
		assertThat(nodeList.getItems()).as("Node List").are(publishingDirSegment);
	}
}
