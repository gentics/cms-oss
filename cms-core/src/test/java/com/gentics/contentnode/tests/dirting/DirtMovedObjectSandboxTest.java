package com.gentics.contentnode.tests.dirting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test cases for moving objects between nodes
 */
@RunWith(value = Parameterized.class)
@GCNFeature(unset = { Feature.TAG_IMAGE_RESIZER })
public class DirtMovedObjectSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Setting of the source node
	 */
	protected PublishTarget sourceNodeSetting;

	/**
	 * Setting of the target node
	 */
	protected PublishTarget targetNodeSetting;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: from {0} to {1}")
	public static Collection<Object[]> data() {
		return getTestParameters();
	}

	/**
	 * Create a test instance
	 * @param sourceNodeSetting source node setting
	 * @param targetNodeSetting target node setting
	 */
	public DirtMovedObjectSandboxTest(PublishTarget sourceNodeSetting, PublishTarget targetNodeSetting) {
		this.sourceNodeSetting = sourceNodeSetting;
		this.targetNodeSetting = targetNodeSetting;
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

		for (PublishTarget sourceNode : PublishTarget.values()) {
			for (PublishTarget targetNode : PublishTarget.values()) {
				testData.add(new Object[] { sourceNode, targetNode});
			}
		}
		// testData.add(new Object[] {PublishTarget.FILESYSTEM, PublishTarget.FILESYSTEM});
		return testData;
	}

	@Before
	public void setup() throws Exception {
		PortalConnectorFactory.destroy();
	}

	/**
	 * Create a new Node
	 * 
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @param publishTarget publish target setting
	 * @return node
	 * @throws Exception
	 */
	public Node createNode(String hostName, String name, PublishTarget publishTarget) throws Exception {
		Node node = ContentNodeTestDataUtils.createNode(hostName, name, publishTarget, false, false);

		if (publishTarget.isPublishCR()) {
			datasources.put(ObjectTransformer.getInteger(node.getId(), null), node.getContentMap().getDatasource());
		}

		return node;
	}

	
	/**
	 * Test move a page
	 */
	@Test
	public void testMovePage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create source and target node
		Node source = createNode("source", "Source Node", sourceNodeSetting);
		Node target = createNode("target", "Target Node", targetNodeSetting);

		// create a template in the source node
		Template template = t.createObject(Template.class);

		template.setMlId(1);
		template.setName("Template");
		template.setSource("Dummy template");
		template.getFolders().add(source.getFolder());
		template.save();
		t.commit(false);

		// create a page
		final Page page = t.createObject(Page.class);

		page.setFolderId(source.getFolder().getId());
		page.setTemplateId(template.getId());
		page.setName("Testpage");
		page.save();
		page.publish();
		t.commit(false);

		// run the publish process
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());
		t = TransactionManager.getCurrentTransaction();

		// check whether the page was published as expected
		assertPageInFilesystem(source, page, sourceNodeSetting.isPublishFS());
		if (sourceNodeSetting.isPublishCR()) {
			assertPageInContentrepository(source, page, true);
		}
		assertPageInFilesystem(target, page, false);
		if (targetNodeSetting.isPublishCR()) {
			assertPageInContentrepository(target, page, false);
		}

		// check that exactly one active publish table entry exists for the page
		final List<Integer> nodeIds = new ArrayList<Integer>();

		DBUtils.executeStatement("SELECT node_id FROM publish WHERE page_id = ? AND active = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ObjectTransformer.getInt(page.getId(), 0));
				stmt.setInt(2, 1);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					nodeIds.add(rs.getInt("node_id"));
				}
			}
		});
		assertTrue("Page must be published into source node", nodeIds.contains(source.getId()));
		assertEquals("Check # of publish table entries", 1, nodeIds.size());

		// move the page
		movePage(page, target.getFolder());

		// run publish process again
		testContext.getContext().startTransaction();
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, testContext.getContext().publish(false).getReturnCode());
		t = TransactionManager.getCurrentTransaction();

		// check whether page was publishes as expected
		assertPageInFilesystem(source, page, false);
		if (sourceNodeSetting.isPublishCR()) {
			assertPageInContentrepository(source, page, false);
		}
		assertPageInFilesystem(target, page, targetNodeSetting.isPublishFS());
		if (targetNodeSetting.isPublishCR()) {
			assertPageInContentrepository(target, page, true);
		}

		// check publish table entries
		nodeIds.clear();
		DBUtils.executeStatement("SELECT node_id FROM publish WHERE page_id = ? AND active = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ObjectTransformer.getInt(page.getId(), 0));
				stmt.setInt(2, 1);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					nodeIds.add(rs.getInt("node_id"));
				}
			}
		});
		assertTrue("Page must be published into source node", nodeIds.contains(target.getId()));
		assertEquals("Check # of publish table entries", 1, nodeIds.size());
	}

	/**
	 * Move the given page into the given folder
	 * @param page page
	 * @param target folder
	 * @throws NodeException
	 */
	public void movePage(Page page, Folder target) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		// TODO move the page (currently this is done directly, since moving is not implemented in Java yet)
		Folder source = page.getFolder();

		DBUtils.executeUpdate("UPDATE page SET folder_id = ? WHERE id = ?", new Object[] { target.getId(), page.getId()});
		t.addTransactional(new TransactionalTriggerEvent(page, new String[] { ObjectTransformer.getString(source.getNode().getId(), null)}, Events.MOVE));
		t.addTransactional(new TransactionalTriggerEvent(source, new String[] { "pages"}, Events.UPDATE));
		t.addTransactional(new TransactionalTriggerEvent(target, new String[] { "pages"}, Events.UPDATE));
		ActionLogger.logCmd(ActionLogger.MOVE, Page.TYPE_PAGE, page.getId(), target.getId(), "Folder.move()");
		t.commit(false);
	}

	/**
	 * Assert existence/nonexistence of the published page for the given node
	 * @param node node
	 * @param page page
	 * @param expected true if the page is expected to exist, false if it is expected to not exist
	 * @throws Exception
	 */
	public void assertPageInFilesystem(Node node, Page page, boolean expected) throws Exception {
		Folder folder = page.getFolder();
		java.io.File nodeBaseDir = new java.io.File(testContext.getPubDir(), node.getHostname());
		java.io.File nodePubDir = new java.io.File(nodeBaseDir, node.getPublishDir());
		java.io.File folderPubDir = new java.io.File(nodePubDir, folder.getPublishDir());
		java.io.File publishedFile = new java.io.File(folderPubDir, page.getFilename());

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
	public void assertPageInContentrepository(Node node, Page page, boolean expected) throws Exception {
		Datasource datasource = datasources.get(node.getId());

		assertNotNull(node + " has no datasource", datasource);
		Resolvable crPage = PortalConnectorFactory.getContentObject("10007." + page.getId(), datasource);

		if (expected) {
			assertNotNull("Page " + page + " must exist in cr of " + node, crPage);
		} else {
			assertNull("Page " + page + " must not exist in cr of " + node, crPage);
		}
	}
}
