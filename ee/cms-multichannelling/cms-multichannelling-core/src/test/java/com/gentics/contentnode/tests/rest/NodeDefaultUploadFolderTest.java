package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Test cases for handling the default upload folders for a node
 */
@GCNFeature(set = Feature.MULTICHANNELLING)
public class NodeDefaultUploadFolderTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Folder folder1;
	private static Folder folder2;
	private static Node channel;
	private static Folder localFolder;
	private static Node otherNode;
	private static Folder otherFolder;

	/**
	 * Lambda for setting default upload folder id over the REST API
	 */
	private static BiConsumer<Node, List<Integer>> set = (n, ids) -> {
		NodeResourceImpl resource = new NodeResourceImpl();
		NodeSaveRequest request = new NodeSaveRequest();
		com.gentics.contentnode.rest.model.Node restNode = new com.gentics.contentnode.rest.model.Node();
		request.setNode(restNode);
		restNode.setEditorVersion(0);
		if (ids.size() >= 1) {
			restNode.setDefaultFileFolderId(ids.get(0));
		}
		if (ids.size() >= 2) {
			restNode.setDefaultImageFolderId(ids.get(1));
		}
		ContentNodeRESTUtils.assertResponseOK(resource.update(n.getId().toString(), request));
	};

	/**
	 * Lambda for setting default file upload folder id over REST API and returning the GenericResponse
	 */
	private static Function<Integer, GenericResponse> setFile = (id) -> {
		NodeResourceImpl resource = new NodeResourceImpl();
		NodeSaveRequest request = new NodeSaveRequest();
		com.gentics.contentnode.rest.model.Node restNode = new com.gentics.contentnode.rest.model.Node();
		request.setNode(restNode);
		restNode.setEditorVersion(0);
		restNode.setDefaultFileFolderId(id);
		return resource.update(node.getId().toString(), request);
	};

	/**
	 * Lambda for setting default image upload folder id over REST API and returning the GenericResponse
	 */
	private static Function<Integer, GenericResponse> setImage = (id) -> {
		NodeResourceImpl resource = new NodeResourceImpl();
		NodeSaveRequest request = new NodeSaveRequest();
		com.gentics.contentnode.rest.model.Node restNode = new com.gentics.contentnode.rest.model.Node();
		request.setNode(restNode);
		restNode.setEditorVersion(0);
		restNode.setDefaultImageFolderId(id);
		return resource.update(node.getId().toString(), request);
	};

	/**
	 * Lambda for loading the node over the REST API
	 */
	private static Function<Node, NodeLoadResponse> load = (n) -> {
		NodeResourceImpl resource = new NodeResourceImpl();
		NodeLoadResponse response = resource.get(n.getId().toString(), false);
		ContentNodeRESTUtils.assertResponseOK(response);
		return response;
	};

	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> update(ContentNodeTestDataUtils.createNode(), upd -> {
			upd.setHostname("hode");
		}));
		folder1 = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder 1"));
		folder2 = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder 2"));

		channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));
		localFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Local Folder", channel));

		otherNode = Trx.supply(() -> update(ContentNodeTestDataUtils.createNode(), upd -> {
			upd.setHostname("otherNode");
		}));
		otherFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(otherNode.getFolder(), "Folder"));
	}

	@Before
	public void setup() throws NodeException {
		Function<Node, Node> unset = (n) -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				Node editableNode = t.getObject(n, true);
				editableNode.setDefaultFileFolder(null);
				editableNode.setDefaultImageFolder(null);
				editableNode.save();
				return t.getObject(n);
		};
		node = Trx.execute(unset, node);
		channel = Trx.execute(unset, channel);
		otherNode = Trx.execute(unset, otherNode);
	}

	/**
	 * Test setting file and image to different folders
	 * @throws NodeException
	 */
	@Test
	public void testSetDifferent() throws NodeException {
		Trx.consume(set, node, Arrays.asList(folder1.getId(), folder2.getId()));
		com.gentics.contentnode.rest.model.Node updated = Trx.execute(load, node).getNode();
		assertFolderIds(updated, folder1.getId(), folder2.getId());
	}

	/**
	 * Test setting file and image to the same folder
	 * @throws NodeException
	 */
	@Test
	public void testSetSame() throws NodeException {
		Trx.consume(set, node, Arrays.asList(folder1.getId(), folder1.getId()));
		com.gentics.contentnode.rest.model.Node updated = Trx.execute(load, node).getNode();
		assertFolderIds(updated, folder1.getId(), folder1.getId());
	}

	/**
	 * Test setting file to the root folder
	 * @throws NodeException
	 */
	@Test
	public void testSetRoot() throws NodeException {
		Trx.consume(set, node, Arrays.asList(node.getFolder().getId()));
		com.gentics.contentnode.rest.model.Node updated = Trx.execute(load, node).getNode();
		assertFolderIds(updated, node.getFolder().getId(), null);
	}

	/**
	 * Test setting file and image to a folder of another node
	 * @throws NodeException
	 */
	@Test
	public void testSetOtherNode() throws NodeException {
		Trx.consume(set, node, Arrays.asList(otherFolder.getId(), otherFolder.getId()));
		com.gentics.contentnode.rest.model.Node updated = Trx.execute(load, node).getNode();
		assertFolderIds(updated, otherFolder.getId(), otherFolder.getId());
	}

	/**
	 * Test setting file and image to a local folder (expected to fail)
	 * @throws NodeException
	 */
	@Test
	public void testSetLocalFolder() throws NodeException {
		ContentNodeRESTUtils.assertResponse(Trx.execute(setFile, localFolder.getId()), ResponseCode.INVALIDDATA, "node saving failed", new Message(
				Message.Type.CRITICAL, new CNI18nString("rest.node.invalid.folderid").toString()));
		ContentNodeRESTUtils.assertResponse(Trx.execute(setImage, localFolder.getId()), ResponseCode.INVALIDDATA, "node saving failed", new Message(
				Message.Type.CRITICAL, new CNI18nString("rest.node.invalid.folderid").toString()));
	}

	/**
	 * Test setting file and image of the channel to inherited folders
	 * @throws NodeException
	 */
	@Test
	public void testSetInChannel() throws NodeException {
		Trx.consume(set, channel, Arrays.asList(folder1.getId(), folder2.getId()));
		com.gentics.contentnode.rest.model.Node updated = Trx.execute(load, channel).getNode();
		assertFolderIds(updated, folder1.getId(), folder2.getId());
	}

	/**
	 * Test unset
	 * @throws NodeException
	 */
	@Test
	public void testUnset() throws NodeException {
		Trx.consume(set, node, Arrays.asList(folder1.getId(), folder2.getId()));
		Trx.consume(set, node, Arrays.asList(0, 0));
		com.gentics.contentnode.rest.model.Node updated = Trx.execute(load, node).getNode();
		assertFolderIds(updated, null, null);
	}

	/**
	 * Test saving without changing
	 * @throws NodeException
	 */
	@Test
	public void testUnchanged() throws NodeException {
		Trx.consume(set, node, Arrays.asList(folder1.getId(), folder2.getId()));
		Trx.consume(set, node, Arrays.asList(null, null));
		com.gentics.contentnode.rest.model.Node updated = Trx.execute(load, node).getNode();
		assertFolderIds(updated, folder1.getId(), folder2.getId());
	}

	/**
	 * Assert the folder IDs
	 * @param restNode rest model of the node
	 * @param expectedDefaultFileFolderId expected default file folder id
	 * @param expectedDefaultImageFolderId expected default image folder id
	 */
	protected void assertFolderIds(com.gentics.contentnode.rest.model.Node restNode, Integer expectedDefaultFileFolderId, Integer expectedDefaultImageFolderId) {
		assertEquals("Check defaultFileFolderId", expectedDefaultFileFolderId, restNode.getDefaultFileFolderId());
		assertEquals("Check defaultImageFolderId", expectedDefaultImageFolderId, restNode.getDefaultImageFolderId());
	}
}
