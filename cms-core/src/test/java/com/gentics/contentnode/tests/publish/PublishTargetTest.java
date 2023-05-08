package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for moving objects between nodes
 */
@RunWith(value = Parameterized.class)
@GCNFeature(unset = { Feature.TAG_IMAGE_RESIZER })
public class PublishTargetTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Setting of the source node
	 */
	protected PublishTarget publishTarget;

	protected PublishType publishType;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: publishTarget: {0} publishType {1}")
	public static Collection<Object[]> data() {
		return getTestParameters();
	}

	/**
	 * Create a test instance
	 * @param sourceNodeSetting source node setting
	 * @param targetNodeSetting target node setting
	 */
	public PublishTargetTest(PublishTarget publishTarget, PublishType publishType) {
		this.publishTarget = publishTarget;
		this.publishType   = publishType;
	}

	/**
	 * Map of datasources per node/channel
	 */
	protected Map<Integer, Datasource> datasources = new HashMap<Integer, Datasource>();

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	public static Collection<Object[]> getTestParameters() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (PublishTarget publishTarget : PublishTarget.values()) {
			for (PublishType publishType : PublishType.values()) {
				testData.add(new Object[] { publishTarget, publishType});
			}
		}

		return testData;
	}

	@Before
	public void setup() throws Exception {
		PortalConnectorFactory.destroy();
	}

	/**
	 * Test publishing different object types into the filesystem or content repository.
	 */
	@Test
	public void testPublishTargets() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create a node
		Node node = ContentNodeTestDataUtils.createNode("source", "Source Node", PublishTarget.BOTH);
		datasources.put(ObjectTransformer.getInteger(node.getId(), null), node.getContentMap().getDatasource());
		node = t.getObject(node, true);

		node.setPublishFilesystem(publishTarget.isPublishFS());
		node.setPublishContentmap(publishTarget.isPublishCR());

		node.setPublishFilesystemPages(false);
		node.setPublishContentMapPages(false);
		node.setPublishFilesystemFiles(false);
		node.setPublishContentMapFiles(false);
		node.setPublishContentMapFolders(false);

		switch (publishType) {
		case PAGES:
			node.setPublishFilesystemPages(true);
			node.setPublishContentMapPages(true);
			break;
		case FILES:
			node.setPublishFilesystemFiles(true);
			node.setPublishContentMapFiles(true);
			break;
		case FOLDERS:
			node.setPublishContentMapFolders(true);
			break;
		case ALL:
			node.setPublishFilesystemPages(true);
			node.setPublishContentMapPages(true);
			node.setPublishFilesystemFiles(true);
			node.setPublishContentMapFiles(true);
			node.setPublishContentMapFolders(true);
			break;
		}
		node.save();
		t.commit(false);

		Page page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "page");
		page = t.getObject(page, true);
		page.publish();
		t.commit(false);

		// Create a file
		File file = ContentNodeTestDataUtils.createFile(node.getFolder(), "file.txt", "content".getBytes());

		// Run the publish process
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());
		t = TransactionManager.getCurrentTransaction();

		// Check whether the objects were published as expected
		assertObjectInFilesystem(node, page.getFilename(), page.getFolder(),
				publishTarget.isPublishFS() && (publishType == PublishType.PAGES || publishType == PublishType.ALL));
		assertObjectInFilesystem(node, file.getFilename(), file.getFolder(),
				publishTarget.isPublishFS() && (publishType == PublishType.FILES || publishType == PublishType.ALL));

		assertObjectInContentrepository(node, "10007." + page.getId(),
				publishTarget.isPublishCR() && (publishType == PublishType.PAGES || publishType == PublishType.ALL));
		assertObjectInContentrepository(node, "10008." + file.getId(),
				publishTarget.isPublishCR() && (publishType == PublishType.FILES || publishType == PublishType.ALL));
		assertObjectInContentrepository(node, "10002." + node.getFolder().getId(),
				publishTarget.isPublishCR() && (publishType == PublishType.FOLDERS || publishType == PublishType.ALL));
	}

	/**
	 * Assert existence/nonexistence of the published object for the given node
	 * @param node node
	 * @param page page
	 * @param expected true if the object is expected to exist, false if it is expected to not exist
	 * @throws Exception
	 */
	public void assertObjectInFilesystem(Node node, String filename, Folder folder,
			boolean expected) throws Exception {
		java.io.File nodeBaseDir = new java.io.File(testContext.getPubDir(), node.getHostname());
		java.io.File nodePubDir = new java.io.File(nodeBaseDir, node.getPublishDir());
		java.io.File folderPubDir = new java.io.File(nodePubDir, folder.getPublishDir());
		java.io.File publishedFile = new java.io.File(folderPubDir, filename);

		if (expected) {
			assertTrue("File " + publishedFile + " must exist", publishedFile.exists());
		} else {
			assertFalse("File " + publishedFile + " must not exist", publishedFile.exists());
		}
	}

	/**
	 * Assert existence/nonexistence of the published page in the contentrepository of the given node
	 * @param node node
	 * @param page page
	 * @param expected true if the page is expected to exist
	 * @throws Exception
	 */
	public void assertObjectInContentrepository(Node node, String contentId,
			boolean expected) throws Exception {
		Datasource datasource = datasources.get(node.getId());

		assertNotNull(node + " has no datasource", datasource);
		Resolvable crPage = PortalConnectorFactory.getContentObject(contentId, datasource);

		if (expected) {
			assertNotNull("Object " + contentId + " must exist in cr of " + node, crPage);
		} else {
			assertNull("Object " + contentId + " must not exist in cr of " + node, crPage);
		}
	}

	/**
	 * Possible publish targets
	 */
	public enum PublishType {
		PAGES,
		FILES,
		FOLDERS,
		ALL;
	}
}
