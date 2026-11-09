package com.gentics.contentnode.tests.factory;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class PageLoadTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Master node
	 */
	private static Node node;

	/**
	 * Channel node
	 */
	private static Node channel;

	/**
	 * Page in node, that is inherited in the channel
	 */
	private static Page inherited;

	/**
	 * Page in node, that is localized in the channel
	 */
	private static Page overwritten;

	/**
	 * Localized copy of the page {@link #overwritten} in the channel
	 */
	private static Page localized;

	/**
	 * Channel local page
	 */
	private static Page local;

	/**
	 * Scope for testing
	 */
	@Parameter(0)
	public Scope scope;

	/**
	 * Flag for enabling/disabling cache
	 */
	@Parameter(1)
	public boolean cache;

	/**
	 * Flag for fetching editable pages
	 */
	@Parameter(2)
	public boolean editable;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: scope {0}, cache {1}, editable {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (Scope scope : Scope.values()) {
			for (boolean cache : Arrays.asList(true, false)) {
				for (boolean editable : Arrays.asList(true, false)) {
					data.add(new Object[] {scope, cache, editable});
				}
			}
		}
		return data;
	}

	/**
	 * Setup the test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("Node", "node", "/Content.Node", null, false, false));
		channel = supply(() -> createChannel(node, "Channel", "channel", "/Content.Node"));

		// create template
		Template template = supply(() -> create(Template.class, create -> {
			create.setMlId(1);
			create.setName("Template");
			create.setSource("Template source");
			create.getFolders().add(node.getFolder());
		}));

		inherited = supply(() -> createPage(node, node.getFolder(), template));
		overwritten = supply(() -> createPage(node, node.getFolder(), template));
		localized = supply(() -> localizePage(overwritten, channel));
		local = supply(() -> createPage(channel, node.getFolder(), template));
	}

	/**
	 * Create a page in the given node
	 * @param node node
	 * @param folder folder
	 * @param template template
	 * @return new page
	 * @throws NodeException
	 */
	protected static Page createPage(Node node, Folder folder, Template template) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page newPage = t.createObject(Page.class);
		newPage.setFolderId(folder.getId());
		newPage.setTemplateId(template.getId());
		if (node.isChannel()) {
			newPage.setChannelInfo(node.getId(), newPage.getChannelSetId());
		}
		newPage.save();
		t.commit(false);

		return t.getObject(Page.class, newPage.getId());
	}

	/**
	 * Localize the given page in the channel
	 * @param page page
	 * @param channel channel
	 * @return localized copy
	 * @throws NodeException
	 */
	protected static Page localizePage(Page page, Node channel) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page copy = (Page)page.copy();
		copy.setChannelInfo(channel.getId(), page.getChannelSetId());
		copy.save();
		t.commit(false);

		return t.getObject(Page.class, copy.getId());
	}

	/**
	 * Setup the test cases
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		operate(Transaction::clearNodeObjectCache);
	}

	/**
	 * Test loading pages by id's in different scopes
	 * @throws Exception
	 */
	@Test
	public void testLoadPages() throws Exception {
		if (cache) {
			operate(t -> {
				t.getObject(Page.class, inherited.getId());
				t.getObject(Page.class, overwritten.getId());
				t.getObject(Page.class, localized.getId());
				t.getObject(Page.class, local.getId());
			});
		}

		operate(t -> {
			switch (scope) {
			case MASTER:
				t.setChannelId(node.getId());
				break;
			case CHANNEL:
				t.setChannelId(channel.getId());
				break;
			default:
				break;
			}

			Set<Integer> pageIds = new HashSet<Integer>();
			pageIds.add(ObjectTransformer.getInteger(inherited.getId(), null));
			pageIds.add(ObjectTransformer.getInteger(overwritten.getId(), null));
			pageIds.add(ObjectTransformer.getInteger(localized.getId(), null));
			pageIds.add(ObjectTransformer.getInteger(local.getId(), null));

			List<Page> loadedPages = t.getObjects(Page.class, pageIds, editable);

			assertEquals("Check whether inherited page was fetched", true, loadedPages.contains(inherited));
			assertEquals("Check whether overwritten page was fetched", Arrays.asList(Scope.MASTER, Scope.NONE).contains(scope), loadedPages.contains(overwritten));
			assertEquals("Check whether localized page was fetched", Arrays.asList(Scope.CHANNEL, Scope.NONE).contains(scope), loadedPages.contains(localized));
			assertEquals("Check whether local page was fetched", Arrays.asList(Scope.CHANNEL, Scope.NONE).contains(scope), loadedPages.contains(local));

			for (Page loadedPage : loadedPages) {
				assertEquals("Check editable state of loaded " + loadedPage, editable, loadedPage.getObjectInfo().isEditable());
			}
		});
	}

	/**
	 * Possible scopes
	 */
	public static enum Scope {
		/**
		 * no channelId set
		 */
		NONE,

		/**
		 * channelId set for the master
		 */
		MASTER,

		/**
		 * channelId set for the channel
		 */
		CHANNEL
	}
}
