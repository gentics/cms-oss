package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for deleting nodes
 */
public class DeleteNodePublishTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test whether the publish entries of deleted nodes are removed
	 * @throws Exception
	 */
	@Test
	public void testDeletePublishEntries() throws Exception {
		Node node = null;
		Page page = null;
		try (Trx trx = new Trx(null, 1)) {
			// create node, template and page
			node = ContentNodeTestDataUtils.createNode("host", "Node", PublishTarget.FILESYSTEM);
			page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Testpage");

			// publish the page
			TransactionManager.getCurrentTransaction().getObject(Page.class, page.getId(), true).publish();
			trx.success();
		}

		// run the publish process
		try (Trx trx = new Trx(null, 1)) {
			testContext.publish(false);
			trx.success();
		}

		// check whether the publish entry for the page exists
		try (Trx trx = new Trx(null, 1)) {
			assertEquals("Check # of publish entries", 1, DBUtils.executeSelectAndCountRows("SELECT id FROM publish WHERE node_id = ?", new Object[] {node.getId()}));
			trx.success();
		}

		// delete the node
		try (Trx trx = new Trx(null, 1)) {
			node.delete();
			trx.success();
		}

		// run the publish process
		try (Trx trx = new Trx(null, 1)) {
			testContext.publish(false);
			trx.success();
		}

		// check whether the publish entry for the page is removed node
		try (Trx trx = new Trx(null, 1)) {
			assertEquals("Check # of publish entries", 0, DBUtils.executeSelectAndCountRows("SELECT id FROM publish WHERE node_id = ?", new Object[] {node.getId()}));
			trx.success();
		}
	}
}
