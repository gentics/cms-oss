package com.gentics.contentnode.tests.publish.mccr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.QueueEntry;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for the maintenance of MCCRs
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.MCCR })
public class CRMaintenanceTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	protected Node node;

	protected Node channel;

	protected Node otherNode;


	@Before
	public void setup() throws Exception {
		node = Trx.supply(() -> {
			try {
				return ContentNodeTestDataUtils.createNode("master", "Master", PublishTarget.CONTENTREPOSITORY, true, false);
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
		channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/", PublishTarget.CONTENTREPOSITORY));
		otherNode = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Node editableNode = t.getObject(otherNode, true);
			editableNode.setPublishContentmap(true);
			editableNode.setContentrepositoryId(node.getContentrepositoryId());
			editableNode.save();
			otherNode = t.getObject(editableNode);
		});
	}

	/**
	 * Test cleaning a CR after objects have been deleted
	 * @throws Exception
	 */
	@Test
	public void testClean() throws Exception {
		// create test data in node
		Folder folderInNode = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder"));
		Page pageInNode = Trx.supply(() -> {
			Page page = ContentNodeTestDataUtils.createTemplateAndPage(folderInNode, "Page");
			TransactionManager.getCurrentTransaction().getObject(page, true).publish();
			return page;
		});
		File fileInNode = Trx.supply(() -> ContentNodeTestDataUtils.createFile(folderInNode, "testfile.txt", "Testfile data".getBytes()));

		// create test data in other node
		Folder folderInOtherNode = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(otherNode.getFolder(), "Folder"));
		Page pageInOtherNode = Trx.supply(() -> {
			Page page = ContentNodeTestDataUtils.createTemplateAndPage(folderInOtherNode, "Page");
			TransactionManager.getCurrentTransaction().getObject(page, true).publish();
			return page;
		});
		File fileInOtherNode = Trx.supply(() -> ContentNodeTestDataUtils.createFile(folderInOtherNode, "testfile.txt", "Testfile data".getBytes()));

		Map<Node, Set<NodeObject>> objects = new HashMap<>();
		objects.put(node, new HashSet<>(Arrays.asList(folderInNode, pageInNode, fileInNode)));
		objects.put(channel, new HashSet<>(Arrays.asList(folderInNode, pageInNode, fileInNode)));
		objects.put(otherNode, new HashSet<>(Arrays.asList(folderInOtherNode, pageInOtherNode, fileInOtherNode)));

		// run a publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert that the objects were published
		assertPublished(objects, true);

		// let the contentrepository be cleaned
		cleanCR();

		// assert that the objects are still published
		assertPublished(objects, true);

		// delete the folders
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.getObject(folderInNode, true).delete(true);
			t.getObject(folderInOtherNode, true).delete(true);
		});

		// assert that the objects are still published
		assertPublished(objects, true);

		// let the contentrepository be cleaned
		cleanCR();

		// assert that the objects were cleaned
		assertPublished(objects, false);
	}

	/**
	 * Assert that the objects are (not) published into the CR
	 * @param objects map of nodes to object sets
	 * @param expected true iff the objects are expected to be published
	 * @throws Exception
	 */
	protected void assertPublished(Map<Node, Set<NodeObject>> objects, boolean expected) throws Exception {
		for (Map.Entry<Node, Set<NodeObject>> entry : objects.entrySet()) {
			for (NodeObject object : entry.getValue()) {
				ContentNodeTestUtils.assertPublishCR(object, entry.getKey(), expected);
			}
		}
	}

	/**
	 * Let the CR be cleaned
	 * @throws Exception
	 */
	protected void cleanCR() throws Exception {
		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			QueueEntry entry = new QueueEntry(t.getUnixTimestamp(), -1, -1, Events.DATACHECK_CR, new String[] {
					Integer.toString(node.getContentrepositoryId()), Boolean.toString(true) }, 0, null);
			entry.store(testContext.getContext().getContentNodeFactory());
			trx.success();
		}

		try (Trx trx = new Trx()) {
			testContext.waitForDirtqueueWorker();
			trx.success();
		}
	}
}
