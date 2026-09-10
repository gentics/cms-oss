package com.gentics.contentnode.tests.wastebin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.ChannelStructure;
import com.gentics.contentnode.testutils.ChannelStructure.Channel;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for handling publishable pages where pages are in the wastebin
 */
public class WastebinPublishablePageTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static ChannelStructure structure;
	private static Template template;

	/**
	 * Create static test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		prefs.setFeature(Feature.WASTEBIN.toString().toLowerCase(), true);
		prefs.setFeature(Feature.PUBLISH_CACHE.toString().toLowerCase(), true);
		structure = new ChannelStructure();
		t.commit(false);
		template = ContentNodeTestDataUtils.createTemplate(structure.getNode(Channel.MASTER).getFolder(), "Source", "Template");
	}

	@Test
	public void testLocalizedInWastebin() throws Exception {
		Folder folder = null;
		Page masterPage = null;
		Page masterPageWithDeletedLocalized = null;
		Page localizedDeletedPage = null;
		Page deletedMasterPage = null;
		Page deletedPage = null;

		try (Trx trx = new Trx()) {
			folder = ContentNodeTestDataUtils.createFolder(structure.getNode(Channel.MASTER).getFolder(), "LocalizedInWastebin");
			masterPage = ContentNodeTestDataUtils.createPage(folder, template, "Other Page");
			masterPageWithDeletedLocalized = ContentNodeTestDataUtils.createPage(folder, template, "Master Page");
			localizedDeletedPage = ContentNodeTestDataUtils.localize(masterPageWithDeletedLocalized, structure.getNode(Channel.CHANNEL));
			deletedMasterPage = ContentNodeTestDataUtils.createPage(folder, template, "Deleted Master");
			ContentNodeTestDataUtils.localize(deletedMasterPage, structure.getNode(Channel.CHANNEL));
			deletedPage = ContentNodeTestDataUtils.createPage(folder, template, "Deleted Page");
			trx.success();
		}

		try (Trx trx = new Trx()) {
			localizedDeletedPage.delete();
			deletedMasterPage.delete();
			deletedPage.delete();
			trx.success();
		}

		try (Trx trx = new Trx(); WastebinFilter wastebin = Wastebin.INCLUDE.set()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			localizedDeletedPage = t.getObject(localizedDeletedPage);
			assertNotNull("Localized page should still be found", localizedDeletedPage);
			assertTrue("Localized page should be in wastebin", localizedDeletedPage.isDeleted());
		}

		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setRenderType(RenderType.getDefaultRenderType(t.getNodeConfig().getDefaultPreferences(), RenderType.EM_PUBLISH, -1));
			t.preparePublishData();

			try (ChannelTrx cTrx = new ChannelTrx(structure.getNode(Channel.MASTER))) {
				ContentNodeTestUtils.assertCollection(
						Arrays.asList(masterPage, masterPageWithDeletedLocalized),
						folder.getPages());
			}

			try (ChannelTrx cTrx = new ChannelTrx(structure.getNode(Channel.CHANNEL))) {
				ContentNodeTestUtils.assertCollection(
						Arrays.asList(masterPage, masterPageWithDeletedLocalized),
						folder.getPages());
			}

			try (ChannelTrx cTrx = new ChannelTrx(structure.getNode(Channel.SUBCHANNEL))) {
				ContentNodeTestUtils.assertCollection(
						Arrays.asList(masterPage, masterPageWithDeletedLocalized),
						folder.getPages());
			}

			trx.success();
		}
	}

	/**
	 * Assert that the given page can still be found in the DB, but is marked deleted
	 * @param page page to check
	 * @throws NodeException
	 */
	protected void assertDeleted(Page page) throws NodeException {
		try (Trx trx = new Trx(); WastebinFilter wastebin = Wastebin.INCLUDE.set()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			assertNotNull(page + " should still be found", t.getObject(page));
			assertTrue(page + " should be in wastebin", t.getObject(page).isDeleted());
		}
	}
}
