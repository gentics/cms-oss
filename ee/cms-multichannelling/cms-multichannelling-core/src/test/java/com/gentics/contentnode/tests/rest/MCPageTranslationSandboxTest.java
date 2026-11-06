package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for translations of pages in channels
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class MCPageTranslationSandboxTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test "translating" a page twice in a channel
	 * @throws Exception
	 */
	@Test
	public void testTranslateTwiceinChannel() throws Exception {
		com.gentics.contentnode.object.Node channel = null;
		com.gentics.contentnode.object.Page page    = null;

		try (Trx trx = new Trx()) {
			ContentLanguage klingon = Creator.createLanguage("klingon", "tlh");
			ContentLanguage tengwar = Creator.createLanguage("tengwar", "teng");

			com.gentics.contentnode.object.Node node
				= ContentNodeTestDataUtils.createNode("node", "node", PublishTarget.NONE, false, false, klingon, tengwar);
			channel = ContentNodeTestDataUtils.createChannel(node, "channel", "channel", "/");
			Folder folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "folder", channel);

			// create a new page
			Template template = ContentNodeTestDataUtils.createTemplate(folder, "template");
			page = ContentNodeTestDataUtils.createPage(folder, template, "newpage", channel, klingon);

			PageResourceImpl pageResource = new PageResourceImpl();
			pageResource.translate(page.getId(), "teng", false, null);
			pageResource.translate(page.getId(), "teng", false, null);
		}

		try (Trx trx = new Trx(); ChannelTrx channelTrx = new ChannelTrx(channel)) {
			page = TransactionManager.getCurrentTransaction().getObject(page);
			assertEquals("There should be 2 language variants only", 2, page.getLanguageVariants(false).size());
		}
	}

}
