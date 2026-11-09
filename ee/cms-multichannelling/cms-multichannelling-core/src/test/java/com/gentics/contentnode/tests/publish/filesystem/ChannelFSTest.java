package com.gentics.contentnode.tests.publish.filesystem;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateAndPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for publishing channels into the filesystem, when the master does not publish into the filesystem
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.MULTITHREADED_PUBLISHING, Feature.NICE_URLS })
public class ChannelFSTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node master;
	private static Node channel;
	private static Folder folder;
	private static Page page;

	@BeforeClass
	public static void setup() throws NodeException {
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (Node node : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				node.delete(true);
			}
		});

		master = Trx.supply(() -> createNode("master", "Master", PublishTarget.CONTENTREPOSITORY));
		channel = Trx.supply(() -> createChannel(master, "Channel", "channel", "/", PublishTarget.FILESYSTEM));

		folder = Trx.supply(() -> createFolder(master.getFolder(), "Folder"));
		page = Trx.supply(() -> createTemplateAndPage(folder, "Page"));
	}

	/**
	 * Test publishing a page
	 * @throws Exception
	 */
	@Test
	public void testPublishPage() throws Exception {
		Trx.operate(() -> update(page, Page::publish));

		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(testContext.getPubDir(), page, channel, true);
			trx.success();
		}
	}
}
