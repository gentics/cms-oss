package com.gentics.contentnode.tests.rest.node;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.Editor;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for creation of nodes
 */
@GCNFeature(set = { Feature.PUB_DIR_SEGMENT })
public class NodeCreationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test creating a node with "pub dir segments"
	 * @throws NodeException
	 */
	@Test
	public void testCreateWithPubDirSegment() throws NodeException {
		Trx.operate(t -> {
			NodeResourceImpl resource = new NodeResourceImpl();
			Node node = new Node();
			node.setName("Node name");
			node.setHost("node.com");
			node.setPubDirSegment(true);
			node.setContentEditor(Editor.AlohaEditor);
			node.setUtf8(true);

			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(node);

			NodeLoadResponse response = resource.add(request);
			ContentNodeRESTUtils.assertResponseOK(response);
		});
	}
}
