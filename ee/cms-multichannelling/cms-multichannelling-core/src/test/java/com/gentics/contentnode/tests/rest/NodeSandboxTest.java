package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getNodeResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.gentics.contentnode.factory.Trx;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.util.FileUtil;

/**
 * Test cases for handling Nodes over the Rest API
 */
@GCNFeature(set = Feature.MULTICHANNELLING)
public class NodeSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	@Before
	public void setUp() throws Exception {
		// disable publish for all nodes
		testContext.getDBSQLUtils().executeQueryManipulation("UPDATE node SET disable_publish = 1, publish_fs = 0, publish_contentmap = 0");
	}

	/**
	 * Test setting and getting default folders for uploading files and images
	 *
	 * @throws Exception
	 */
	@Test
	public void testDefaultFolders() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodeResource nodeResource = getNodeResource();

		Node node = ContentNodeTestDataUtils.createNode("New testing Node", "host", "/Content.Node", null, false, false, false);

		final Integer nodeId = node.getId();

		Folder fileFolder = t.createObject(Folder.class);

		fileFolder.setNodeId(node.getId());
		fileFolder.setName("Default File Folder");
		fileFolder.setPublishDir("/");
		fileFolder.save();
		Folder imageFolder = t.createObject(Folder.class);

		imageFolder.setNodeId(node.getId());
		imageFolder.setName("Default Image Folder");
		imageFolder.setPublishDir("/");
		imageFolder.save();
		t.commit(false);

		final Object fileFolderId = fileFolder.getId();
		final Object imageFolderId = imageFolder.getId();

		node = t.getObject(Node.class, node.getId(), true);
		node.setDefaultFileFolder(fileFolder);
		node.setDefaultImageFolder(imageFolder);
		node.save();
		t.commit(false);

		// clear all caches
		t.clearLevel2Cache();
		PortalCache.getCache(NodeFactory.CACHEREGION).clear();

		// fetch the object from the REST API
		NodeLoadResponse nodeLoad = nodeResource.get(ObjectTransformer.getString(nodeId, null), false);
		assertEquals("Check response code", ResponseCode.OK, nodeLoad.getResponseInfo().getResponseCode());

		assertEquals("Check default file folder", fileFolderId, nodeLoad.getNode().getDefaultFileFolderId());
		assertEquals("Check default image folder", imageFolderId, nodeLoad.getNode().getDefaultImageFolderId());

		// check in the db
		DBUtils.executeStatement("SELECT * FROM node WHERE id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setObject(1, nodeId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					assertEquals("Check default file folder id in DB", fileFolderId, rs.getObject("default_file_folder_id"));
					assertEquals("Check default image folder id in DB", imageFolderId, rs.getObject("default_image_folder_id"));
				} else {
					fail("Did not find data for node " + nodeId);
				}
			}
		});

		// unset
		node = t.getObject(Node.class, nodeId, true);
		node.setDefaultFileFolder(null);
		node.setDefaultImageFolder(null);
		node.save();
		t.commit(false);

		// clear all caches
		t.clearLevel2Cache();
		PortalCache.getCache(NodeFactory.CACHEREGION).clear();

		// refetch object from db
		nodeLoad = nodeResource.get(ObjectTransformer.getString(nodeId, null), false);
		assertEquals("Check response code", ResponseCode.OK, nodeLoad.getResponseInfo().getResponseCode());

		assertNull("Check default file folder", nodeLoad.getNode().getDefaultFileFolderId());
		assertNull("Check default image folder", nodeLoad.getNode().getDefaultImageFolderId());
	}

	/**
	 * Test that a newly created node, uses UTF8 and Aloha editor by default.
	 * @throws NodeException
	 */
	@Test
	public void testDefaultEditor() throws NodeException {
		Node newNode = Trx.supply(t -> {
			Node node = t.createObject(Node.class);
			Folder root = t.createObject(Folder.class);

			root.setName("Test node");
			root.setPublishDir("/");
			node.setFolder(root);
			node.setPublishDir("/");
			node.setHostname("testhost");

			node.save();

			return node;
		});

		assertThat(newNode).as("Created node").isUtf8();
		assertThat(newNode).as("Created node").hasEditorversion(Node.EDITOR_VERSION_ALOHA_EDITOR);
	}

	/**
	 * Test checking whether a node is a channel of another node
	 *
	 * @throws Exception
	 */
	@Test
	public void testIsChannelOf() throws Exception {
		NodeResource nodeResource = getNodeResource();

		Node master = ContentNodeTestDataUtils.createNode("Master", "masterhost", "/", null, false, false, false);
		Node firstChannel = ContentNodeTestDataUtils.createChannel(master, "1. Channel", "firstchannel", "/");
		Node secondChannel = ContentNodeTestDataUtils.createChannel(firstChannel, "2. Channel", "secondchannel", "/");

		NodeLoadResponse nodeLoad = nodeResource.get(ObjectTransformer.getString(master.getId(), null), false);
		assertEquals("Check response code", ResponseCode.OK, nodeLoad.getResponseInfo().getResponseCode());
		assertEquals("The master should have a inherted from id which points to itself.",master.getId(), nodeLoad.getNode().getInheritedFromId());
		assertEquals("The master should have a master id which points to itself.",master.getId(), nodeLoad.getNode().getMasterNodeId());

		nodeLoad = nodeResource.get(ObjectTransformer.getString(firstChannel.getId(), null), false);
		assertEquals("Check response code", ResponseCode.OK, nodeLoad.getResponseInfo().getResponseCode());
		assertEquals("The first channel should have a inherted from id which points to itself.",master.getId(), nodeLoad.getNode().getInheritedFromId());
		assertEquals("The first channel should have a master id which points to the master.",master.getId(), nodeLoad.getNode().getMasterNodeId());

		nodeLoad = nodeResource.get(ObjectTransformer.getString(secondChannel.getId(), null), false);
		assertEquals("Check response code", ResponseCode.OK, nodeLoad.getResponseInfo().getResponseCode());
		assertEquals("The second channel should have a inherted from id which points to the first channel.",firstChannel.getId(), nodeLoad.getNode().getInheritedFromId());
		assertEquals("The second channel should have a master id which points to the master.",master.getId(), nodeLoad.getNode().getMasterNodeId());

		assertFalse("Check whether" + master + " is no channel", master.isChannel());
		assertTrue("Check whether" + firstChannel + " is a channel", firstChannel.isChannel());
		assertTrue("Check whether" + secondChannel + " is a channel", secondChannel.isChannel());

		assertTrue("Check whether " + firstChannel + " is channel of " + master, firstChannel.isChannelOf(master));
		assertTrue("Check whether " + secondChannel + " is channel of " + master, secondChannel.isChannelOf(master));
		assertTrue("Check whether " + secondChannel + " is channel of " + firstChannel, secondChannel.isChannelOf(firstChannel));
		assertFalse("Check whether " + master + " is not a channel of " + firstChannel, master.isChannelOf(firstChannel));
		assertFalse("Check whether " + master + " is not a channel of " + secondChannel, master.isChannelOf(secondChannel));
		assertFalse("Check whether " + firstChannel + " is not a channel of " + secondChannel, firstChannel.isChannelOf(secondChannel));
	}

	/**
	 * Test publishing with separate publish dir and binary publish dir
	 *
	 * @throws Exception
	 */
	@Test
	public void testBinaryPublishDir() throws Exception {
		Node node = ContentNodeTestDataUtils.createNode("Separate Dirs", "host", "/pages", "/binaries", true, false, false);
		Folder rootFolder = node.getFolder();
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a construct
		Construct construct = t.createObject(Construct.class);

		construct.setKeyword("link");
		construct.setName("Link (de)", 1);
		construct.setName("Link (en)", 2);
		construct.getNodes().add(node);
		Part urlPart = t.createObject(Part.class);

		urlPart.setKeyname("url");
		urlPart.setHidden(false);
		urlPart.setEditable(1);
		urlPart.setName("Url", 1);
		urlPart.setName("Url", 2);
		urlPart.setPartOrder(1);
		urlPart.setPartTypeId(8);
		List<Part> parts = construct.getParts();

		parts.add(urlPart);
		construct.save();

		// create a template
		Template template = t.createObject(Template.class);

		template.setFolderId(rootFolder.getId());
		template.setMlId(1);
		template.setName("Test Template");
		template.setSource("<node link1>");
		template.save();
		t.commit(false);

		// create a file
		File file = t.createObject(File.class);

		file.setFolderId(rootFolder.getId());
		file.setName("testfile.txt");
		file.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
		file.save();

		// create a page
		Page page = t.createObject(Page.class);

		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		ContentTag tag = page.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));

		tag.setName("link1");
		tag.setEnabled(true);
		PartType partType = tag.getValues().getByKeyname("url").getPartType();

		if (partType instanceof FileURLPartType) {
			((FileURLPartType) partType).setTargetFile(file);
		} else {
			fail("Wrong parttype " + partType.getClass());
		}
		page.save();
		page.publish();
		t.commit(false);

		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());

		// check that the file exists in the correct place
		assertFile(
				new java.io.File(new java.io.File(new java.io.File(new java.io.File(testContext.getContext().getPubDir(), node.getHostname()), "/pages"), rootFolder.getPublishDir()),
				file.getName()),
				false);
		assertFile(
				new java.io.File(
						new java.io.File(new java.io.File(new java.io.File(testContext.getContext().getPubDir(), node.getHostname()), "/binaries"), rootFolder.getPublishDir()),
						file.getName()),
						true);
		// check that the page rendered the file URL correctly
		java.io.File renderedPage = new java.io.File(
				new java.io.File(new java.io.File(new java.io.File(testContext.getContext().getPubDir(), node.getHostname()), "/pages"), rootFolder.getPublishDir()),
				page.getFilename());

		assertFile(renderedPage, true);
		String pageContent = FileUtil.file2String(renderedPage);

		assertEquals("Check the rendered page", "/binaries/testfile.txt", pageContent);
	}

	/**
	 * Test changing the binary publish directory (check dirting)
	 *
	 * @throws Exception
	 */
	@Test
	public void testChangeBinaryPublishDir() throws Exception {
		Node node = ContentNodeTestDataUtils.createNode("Separate Dirs", "host", "/pages", "/binaries", true, false, false);
		Folder rootFolder = node.getFolder();
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a construct
		Construct construct = t.createObject(Construct.class);

		construct.setKeyword("link");
		construct.setName("Link (de)", 1);
		construct.setName("Link (en)", 2);
		construct.getNodes().add(node);
		Part urlPart = t.createObject(Part.class);

		urlPart.setKeyname("url");
		urlPart.setHidden(false);
		urlPart.setEditable(1);
		urlPart.setName("Url", 1);
		urlPart.setName("Url", 2);
		urlPart.setPartOrder(1);
		urlPart.setPartTypeId(8);
		List<Part> parts = construct.getParts();

		parts.add(urlPart);
		construct.save();

		// create a template
		Template template = t.createObject(Template.class);

		template.setFolderId(rootFolder.getId());
		template.setMlId(1);
		template.setName("Test Template");
		template.setSource("<node link1>");
		template.save();
		t.commit(false);

		// create a file
		File file = t.createObject(File.class);

		file.setFolderId(rootFolder.getId());
		file.setName("testfile.txt");
		file.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
		file.save();

		// create a page
		Page page = t.createObject(Page.class);

		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		ContentTag tag = page.getContent().addContentTag(ObjectTransformer.getInt(construct.getId(), 0));

		tag.setName("link1");
		tag.setEnabled(true);
		PartType partType = tag.getValues().getByKeyname("url").getPartType();

		if (partType instanceof FileURLPartType) {
			((FileURLPartType) partType).setTargetFile(file);
		} else {
			fail("Wrong parttype " + partType.getClass());
		}
		page.save();
		page.publish();
		t.commit(false);

		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());
		t = TransactionManager.getCurrentTransaction();

		// now change the binary publish directory of the node
		node = t.getObject(Node.class, node.getId(), true);
		node.setBinaryPublishDir("/files");
		node.save();
		t.commit(false);

		node = t.getObject(Node.class, node.getId());

		// wait until everything was dirted
		testContext.getContext().waitForDirtqueueWorker(testContext.getDBSQLUtils());

		// run another publish process
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());

		// check that the file exists in the correct place
		assertFile(
				new java.io.File(
						new java.io.File(new java.io.File(new java.io.File(testContext.getContext().getPubDir(), node.getHostname()), "/binaries"), rootFolder.getPublishDir()),
						file.getName()),
						false);
		assertFile(
				new java.io.File(new java.io.File(new java.io.File(new java.io.File(testContext.getContext().getPubDir(), node.getHostname()), "/pages"), rootFolder.getPublishDir()),
				file.getName()),
				false);
		assertFile(
				new java.io.File(new java.io.File(new java.io.File(new java.io.File(testContext.getContext().getPubDir(), node.getHostname()), "/files"), rootFolder.getPublishDir()),
				file.getName()),
				true);
		// check that the page rendered the file URL correctly
		java.io.File renderedPage = new java.io.File(
				new java.io.File(new java.io.File(new java.io.File(testContext.getContext().getPubDir(), node.getHostname()), "/pages"), rootFolder.getPublishDir()),
				page.getFilename());

		assertFile(renderedPage, true);
		String pageContent = FileUtil.file2String(renderedPage);

		assertEquals("Check the rendered page", "/files/testfile.txt", pageContent);
	}

	/**
	 * Assert existence or non-existence of the given file
	 *
	 * @param file
	 *            file
	 * @param expected
	 *            true if the file is expected to exist
	 * @throws Exception
	 */
	protected void assertFile(java.io.File file, boolean expected) throws Exception {
		if (expected) {
			assertTrue(file.getAbsolutePath() + " must exist", file.exists());
		} else {
			assertFalse(file.getAbsolutePath() + " must not exist", file.exists());
		}
	}
}
