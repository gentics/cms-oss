package com.gentics.contentnode.tests.publish.disinherit;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for disinheriting of pages
 */
public class DisinheritPageTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Master node
	 */
	private Node master;

	/**
	 * Channel
	 */
	private Node channel;

	/**
	 * Setup test data
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		master = ContentNodeTestDataUtils.createNode("Node", "Node", PublishTarget.NONE);
		channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "Channel", "/");
	}

	/**
	 * Test cancelling a page, that had been disinherited
	 * @throws Exception
	 */
	@Test
	public void testCancelPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = ContentNodeTestDataUtils.createTemplate(master.getFolder(), "Template");
		Page page = ContentNodeTestDataUtils.createPage(master.getFolder(), template, "Page");

		page.changeMultichannellingRestrictions(false, new HashSet<Node>(Arrays.asList(channel)), false);
		t.commit(false);

		t = testContext.startSystemUserTransaction();
		testContext.getContext().clearNodeObjectCache();

		page = t.getObject(Page.class, page.getId());
		Set<Node> disinheritedChannels = page.getDisinheritedChannels();
		assertEquals("Check # of disinherited channels", 1, disinheritedChannels.size());
		assertEquals("Check disinherited channel", channel, disinheritedChannels.iterator().next());

		getPageResource().cancel(page.getId(), null);
		t = testContext.startSystemUserTransaction();
		testContext.getContext().clearNodeObjectCache();

		page = t.getObject(Page.class, page.getId());
		disinheritedChannels = page.getDisinheritedChannels();
		assertEquals("Check # of disinherited channels", 1, disinheritedChannels.size());
		assertEquals("Check disinherited channel", channel, disinheritedChannels.iterator().next());
	}
}
