package com.gentics.contentnode.tests.publish;

import com.gentics.contentnode.testutils.GCNFeature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionalDirtCache;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;

/**
 * Tests concerning moving objects, so that they get published in the same CR.
 * @author escitalopram
 *
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.MCCR})
public class SameCRMoveTest extends AbstractInstantPublishingTestSandboxTest {
	private boolean sourceAutoOffline;
	private boolean targetAutoOffline;
	private boolean instantPublish;
	private Node source;
	private Node target;
	private boolean mccr;
	private Datasource repository;

	public static int timestampCounter = 1;

	/**
	 * Sets the contentfile_auto_offline feature
	 * @param n node to set the feature on
	 * @return
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	private Node setAutoOffline(Node n) throws ReadOnlyException, NodeException {
		Transaction t = testContext.getContext().getTransaction();
		n = t.getObject(Node.class, n.getId(), true);
		n.activateFeature(Feature.CONTENTFILE_AUTO_OFFLINE);
		n.save();
		return n;
	}

	/**
	 * Check whether the file is in the contentrepository of the given node and as the node_id set correctly, or if the file is not in the cr
	 * @param exists true if the file is expected to exist in the cr, false if not
	 * @param file file to check
	 * @param node node of the file
	 * @throws NodeException
	 * @throws Exception
	 */
	private void assertInContentrepositoryInNode(boolean exists, File file, Node node) throws NodeException, Exception {
		if (exists) {
			assertInContentrepository(node, file, "node_id", node.getId().toString());
		} else {
			assertInContentrepository(node, file, false);
		}
	}

	/**
	 * Check whether the file exists in the cr, if instant publishing is used
	 * @param exists true if the file is expected to exist in the cr, false if not
	 * @param file file to check
	 * @param node node of the file
	 * @throws Exception
	 */
	private void assertInstantPublishingWorked(boolean exists, File file, Node node) throws Exception {
		if (!instantPublish || !exists) {
			return;
		}
		assertInContentrepositoryInNode(true, file, node);
	}

	/**
	 * Test if moved files are properly (instant) published if moved between nodes of the same CR
	 * @throws Exception
	 */
	@Test
	public void testFileMove() throws Exception {
		TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences().setFeature(Feature.CONTENTFILE_AUTO_OFFLINE.toString().toLowerCase(),
				true);

		testContext.startTransaction(timestampCounter++);

		source = createNode("src", "src", instantPublish, false, null, mccr);
		target = createNode("tgt", "tgt", instantPublish, false, null, mccr);

		// set same CR on both nodes
		repository = datasources.get(target.getId());
		source = testContext.getContext().getTransaction().getObject(Node.class, source.getId(),true);
		source.setContentrepositoryId(target.getContentrepositoryId());
		datasources.put(source.getId(), repository);
		source.save();

		testContext.startTransaction(timestampCounter++);

		if (sourceAutoOffline) {
			source = setAutoOffline(source);
		}
		if (targetAutoOffline) {
			target = setAutoOffline(target);
		}

		testContext.startTransaction(timestampCounter++);
		testContext.publish(timestampCounter++);
		testContext.startTransaction(timestampCounter++);

		File file = createFile(source.getFolder(), "test1", new byte[] { 1, 2, 3, 4, 5 });

		testContext.startTransaction(timestampCounter++);

		assertInstantPublishingWorked(!sourceAutoOffline, file, source);

		testContext.publish(timestampCounter++);

		testContext.startTransaction(timestampCounter++);

		assertInContentrepositoryInNode(!sourceAutoOffline, file, source);

		moveFile(file, target);

		// check for successful instant publish
		file = testContext.getContext().getTransaction().getObject(File.class, file.getId());
		assertInstantPublishingWorked(!targetAutoOffline, file, target);

		testContext.publish(timestampCounter++);

		testContext.startTransaction(timestampCounter++);

		assertInContentrepositoryInNode(!targetAutoOffline, file, target);

		moveFile(file, source);

		// check for successful instant publish
		file = testContext.getContext().getTransaction().getObject(File.class, file.getId());
		assertInstantPublishingWorked(!sourceAutoOffline, file, source);

		testContext.publish(timestampCounter++);

		testContext.startTransaction(timestampCounter++);

		assertInContentrepositoryInNode(!sourceAutoOffline, file, source);
	}

	/**
	 * Simulates moving a file in the backend since it is not implemented in Java
	 * @param file File to move
	 * @param target File will be moved to the root folder of this node
	 * @throws NodeException
	 */
	private void moveFile(File file, final Node target) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Integer targetFolderId = target.getFolder().getId();
		DBUtils.executeUpdate("UPDATE contentfile set folder_id = ? where id = ?", new Object[] { targetFolderId, file.getId()});
		t.addTransactional(new TransactionalDirtCache(File.class, file.getId()));
		t.commit(false);
		File newFile = t.getObject(File.class, file.getId());
		Folder sourceFolder = file.getFolder();
		t.addTransactional(new TransactionalTriggerEvent(newFile, new String[]{file.getFolder().getNode().getId().toString()}, Events.MOVE));
		t.addTransactional(new TransactionalTriggerEvent(sourceFolder, new String[]{"files","images"}, Events.UPDATE));
		t.addTransactional(new TransactionalTriggerEvent(target.getFolder(), new String[]{"files","images"}, Events.UPDATE));
		ActionLogger.logCmd(ActionLogger.MOVE, File.TYPE_FILE, file.getId(), targetFolderId, "Folder.move()");
		t.commit(false);
	}

	/**
	 * Create a test instance
	 * @param sourceAutoOffline true if autooffline is on for the source node
	 * @param targetAutoOffline true if autooffline is on for the target node
	 * @param instantPublish true if the cr uses instant publishing
	 * @param mccr true if the cr is a mccr
	 */
	public SameCRMoveTest(boolean sourceAutoOffline, boolean targetAutoOffline, boolean instantPublish, boolean mccr) {
		this.sourceAutoOffline = sourceAutoOffline;
		this.targetAutoOffline = targetAutoOffline;
		this.instantPublish = instantPublish;
		this.mccr = mccr;
	}

	/**
	 * Remove nodes, that were created in the test
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (source != null) {
			source.delete();
			source = null;
		}
		if (target != null) {
			target.delete();
			target = null;
		}
		TransactionManager.getCurrentTransaction().commit(false);
	}

	@Parameters(name = "{index}: srcAutoOffline {0}, tgtAutoOffline: {1}, instantPublish: {2}, mccr: {3}")
	public static Collection<Object[]> parameters() {
		List<Object[]> result = new ArrayList<Object[]>();
		Boolean[] bools = new Boolean[] { Boolean.FALSE, Boolean.TRUE };
		for (Boolean srcAutoOffline : bools) {
			for (Boolean tgtAutoOffline : bools) {
				for (Boolean instantPub : bools) {
					for (Boolean mccr : bools) {
						result.add(new Object[] { srcAutoOffline, tgtAutoOffline, instantPub, mccr });
					}
				}
			}
		}
		return result;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		AbstractInstantPublishingTestSandboxTest.setupOnce();
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTITHREADED_PUBLISHING, true);
		prefs.setProperty("contentnode.config.loadbalancing.threadlimit", "2");
	}
}
