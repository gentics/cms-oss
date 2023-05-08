package com.gentics.contentnode.tests.wastebin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test for deleting a whole node
 */
public class WastebinDeleteNodeTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Static setup (activate feature wastebin)
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.WASTEBIN.toString().toLowerCase(), true);
	}

	/**
	 * Check whether all objects of a node (including objects in the wastebin) are deleted when a node is deleted
	 * @throws Exception
	 */
	@Test
	public void deleteNode() throws Exception {
		Node node;
		Folder folder1, folder2;
		Page page1, page2;
		File file1, file2;

		// create the test data
		try (Trx trx = new Trx(null, 1)) {
			node = ContentNodeTestDataUtils.createNode();
			folder1 = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder1");
			folder2 = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder2");

			page1 = ContentNodeTestDataUtils.createTemplateAndPage(folder1, "Page1");
			page2 = ContentNodeTestDataUtils.createTemplateAndPage(folder2, "Page2");
			file1 = ContentNodeTestDataUtils.createFile(folder1, "file1.txt", "File1 Contents".getBytes());
			file2 = ContentNodeTestDataUtils.createFile(folder2, "file2.txt", "File2 Contents".getBytes());

			trx.success();
		}

		// delete some things (put them into the wastebin)
		try (Trx trx = new Trx(null, 1)) {
			folder2.delete();
			trx.success();
		}

		// check that deleted objects are in wastebin
		try (Trx trx = new Trx(null, 1); WastebinFilter wb = Wastebin.INCLUDE.set()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (NodeObject o : Arrays.asList(folder2, file2, page2)) {
				assertNotNull(o + " must still exist in the wastebin", t.getObject(o));
				assertTrue(o + " must be deleted", t.getObject(o).isDeleted());
			}
			trx.success();
		}

		// delete the node
		try (Trx trx = new Trx(null, 1)) {
			node.delete();
			trx.success();
		}

		// everything must be gone now
		try (Trx trx = new Trx(null, 1); WastebinFilter wb = Wastebin.INCLUDE.set()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (NodeObject o : Arrays.asList(node, folder1, file1, page1, folder2, file2, page2)) {
				assertNull(o + " must be deleted", t.getObject(o));
			}
			trx.success();
		}
	}
}
