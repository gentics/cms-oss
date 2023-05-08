package com.gentics.contentnode.tests.publish.instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;

/**
 * Test cases for instant publishing in mc environments
 */
public class MCInstantPublishingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node master;

	private static Node channel;

	private static Template template;

	/**
	 * Setup common test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		prefs.setFeature("mccr", true);
		prefs.setFeature("instant_cr_publishing", true);

		master = ContentNodeTestDataUtils.createNode("master", "Master", PublishTarget.CONTENTREPOSITORY, true, false);
		channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "channel", "/");
		channel = t.getObject(Node.class, channel.getId(), true);
		channel.setContentrepositoryId(master.getContentrepositoryId());
		channel.setPublishContentmap(true);
		channel.save();
		t.commit(false);
		channel = t.getObject(Node.class, channel.getId());

		// make the CR instant publishing
		ContentRepository cr = t.getObject(ContentRepository.class, master.getContentrepositoryId(), true);
		cr.setInstantPublishing(true);
		cr.save();
		t.commit(false);

		template = t.createObject(Template.class);
		template.setFolderId(master.getFolder().getId());
		template.setMlId(1);
		template.setName("Template");
		template.setSource("Source");
		template.save();
		t.commit(false);

		template = t.getObject(Template.class, template.getId());
	}

	/**
	 * Test taking the localized copy of a page offline
	 * @throws Exception
	 */
	@Test
	public void testTakeLocalizedCopyOffline() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page masterPage = ContentNodeTestDataUtils.createPage(master.getFolder(), template, "Page");
		t.getObject(Page.class, masterPage.getId(), true).publish();
		t.commit(false);

		Page localizedCopy = ContentNodeTestDataUtils.localize(masterPage, channel);
		t.getObject(Page.class, localizedCopy.getId(), true).publish();
		t.commit(false);

		testContext.publish(false);

		t = testContext.startSystemUserTransaction();

		assertPage(masterPage, master, masterPage);
		assertPage(masterPage, channel, localizedCopy);
		assertPage(localizedCopy, master, null);
		assertPage(localizedCopy, channel, localizedCopy);

		t.getObject(Page.class, localizedCopy.getId(), true).takeOffline();
		t.commit(false);

		assertPage(masterPage, master, masterPage);
		assertPage(masterPage, channel, null);
		assertPage(localizedCopy, master, null);
		assertPage(localizedCopy, channel, null);
	}

	/**
	 * Assert whether the page is published into the contentrepository of the given node/channel
	 * @param searchPage searched page
	 * @param node node
	 * @param expectedPage expected page
	 * @throws Exception
	 */
	protected void assertPage(Page searchPage, Node node, Page expectedPage) throws Exception {
		WritableMCCRDatasource ds = node.getContentMap().getMCCRDatasource();
		ds.setChannel(ObjectTransformer.getInt(node.getId(), 0));
		Resolvable resolvable = PortalConnectorFactory.getContentObject(Page.TYPE_PAGE + "." + searchPage.getId(), ds);
		if (expectedPage == null) {
			assertNull(searchPage + " was not expected to be published into " + node, resolvable);
		} else {
			String contentId = null;
			if (resolvable != null) {
				contentId = ObjectTransformer.getString(resolvable.get("contentid"), null);
			}
			assertEquals("Check found page", Page.TYPE_PAGE + "." + expectedPage.getId(), contentId);
		}
	}
}
