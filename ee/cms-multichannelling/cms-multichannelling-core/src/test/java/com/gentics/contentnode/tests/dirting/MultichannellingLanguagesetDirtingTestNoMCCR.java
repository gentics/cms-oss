package com.gentics.contentnode.tests.dirting;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.PoolConnection;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test dirting of changes in the languageset in a multichannelling context, using a channel tree with a master node and 5 channels.
 * @author escitalopram
 *
 */
@RunWith(Parameterized.class)
public class MultichannellingLanguagesetDirtingTestNoMCCR {

	public enum ChannelRelationship {
		NONE(0),
		DIRECT(1),
		INDIRECT(2);

		private int extraHeight;

		private ChannelRelationship(int extraHeight) {
			this.extraHeight = extraHeight;
		}

		/**
		 * Returns the number of levels of channels needed to add a channel with this relationship to the parent channel.
		 * @return
		 */
		public int getExtraHeight() {
			return extraHeight;
		}
	}

	/**
	 * Represents a set of localization patterns for an object and an accompanying channel specification for an associated object (e.g. a translation)
	 * @author escitalopram
	 *
	 */
	static class FixedChannelSubset {
		private final int changeableChannel;
		private final List<Integer> localizations;

		/**
		 * Create a new FixedChanelSubset
		 * @param predecessor the changeable channel has this relation to a parent channel node
		 * @param changeableChannelLocalized whether the changeable channel should be localized in the fixed channelset
		 * @param successor the relationship of the next localized object down the channel tree to the changeable channel
		 */
		public FixedChannelSubset(ChannelRelationship predecessor, boolean changeableChannelLocalized, ChannelRelationship successor) {
			this.changeableChannel = predecessor.getExtraHeight();
			localizations = new ArrayList<Integer>();
			int height = predecessor.getExtraHeight();
			if (height > 0 && changeableChannelLocalized) {
				localizations.add(height);
			}
			if (successor != ChannelRelationship.NONE) {
				height += successor.getExtraHeight();
				localizations.add(height);
			}
		}

		/**
		 * The channels with these indices need to be localized in the fixed channelset
		 * @return
		 */
		public List<Integer> getChannelsWithLocalizations() {
			return localizations;
		}

		/**
		 * @return the changeable channel in the changeable channelset
		 */
		public int getChangeableChannel() {
			return changeableChannel;
		}

		@Override
		public String toString() {
			return "ChannelSubset{Changeable: " + changeableChannel + ", Localizations: " + localizations + "}";
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof FixedChannelSubset) {
				return this.toString().equals(obj.toString());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}

		/**
		 * Returns a list of different variants of channels localized in the changeable channelset.
		 * @return list of different indices that can be localized in the changeable channelset. May contain null for "no localization".
		 */
		public List<Integer> getTranslationLocalizationIndices() {
			List<Integer> result = new ArrayList<Integer>();
			for (ChannelRelationship subchannel : ChannelRelationship.values()) {
				Integer currentChannelIndex = null;
				if (subchannel != ChannelRelationship.NONE) {
					currentChannelIndex = changeableChannel + subchannel.getExtraHeight();
				}
				result.add(currentChannelIndex);
			}
			return result;
		}
	}
	private static Node[] nodes = new Node[6];

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@Parameterized.Parameters(name = "{index}: {0}, translationLocalization: {1}")
	public static Collection<Object[]> parameters() {
		Set<FixedChannelSubset> fixedSubsets = new LinkedHashSet<FixedChannelSubset>();
		for (ChannelRelationship pred : ChannelRelationship.values()) {
			for (ChannelRelationship succ : ChannelRelationship.values()) {
				fixedSubsets.add(new FixedChannelSubset(pred, false, succ));
				fixedSubsets.add(new FixedChannelSubset(pred, true, succ));
			}
		}
		List<Object[]> result = new ArrayList<Object[]>();
		for (FixedChannelSubset subset : fixedSubsets) {
			for (Integer variant : subset.getTranslationLocalizationIndices()) {
				result.add(new Object[] { subset, variant });
			}
		}
		return result;
	}

	private FixedChannelSubset fixed;
	private Integer translationLocalization;

	private static ContentLanguage klingon;
	private static ContentLanguage interlingua;
	private static Template template;
	private static int counter = 0;

	private Page originalPage;
	private Page translatedPage;

	public MultichannellingLanguagesetDirtingTestNoMCCR(FixedChannelSubset fixed, Integer translationLocalization) {
		this.fixed = fixed;
		this.translationLocalization = translationLocalization;
		synchronized (this) {
			counter++;
		}
	}

	/**
	 * Return a name unique to each test case
	 * @return the name
	 */
	public String getName() {
		return "x" + counter + "y";
	}

	@BeforeClass
	public static void setupClass() throws Exception {
		setupBaseObjects(false);
	}

	/**
	 * Set up a channel hierarchy with languages and content repositories as needed and a template.
	 * @param withMCCR
	 * @throws Exception
	 */
	public static void setupBaseObjects(boolean withMCCR) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = context.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		klingon = Creator.createLanguage("klingon", "tlh");
		interlingua = Creator.createLanguage("interlingua", "ina");
		nodes[0] = ContentNodeTestDataUtils.createNode("lsdtest.local", "lsdtest", PublishTarget.CONTENTREPOSITORY,
				withMCCR, false, klingon, interlingua);
		adjustContentMap(nodes[0], false);
		for (int i = 1; i < nodes.length; ++i) {
			nodes[i] = ContentNodeTestDataUtils.createChannel(nodes[i - 1], "n" + i, "h" + i, "/", withMCCR ? PublishTarget.NONE : PublishTarget.CONTENTREPOSITORY);
			adjustContentMap(nodes[i], withMCCR);
			nodes[i] = t.getObject(Node.class, nodes[i].getId(), false, false, false);
		}
		template = Creator.createTemplate("tpl", "-", nodes[0].getFolder());
	}

	/**
	 * Take a page or a localization online and offline and check the dirting result.
	 * @throws Exception
	 */
	@Test
	public void testOnlineOffline() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		// Create fixed contentset
		originalPage = Creator.createPage(getName(), nodes[0].getFolder(), template, klingon);
		createLocalizations(originalPage, fixed.getChannelsWithLocalizations());
		originalPage.publish();
		t.commit(false);

		// Create changing-contentset
		PageLoadResponse translateResponse = getPageResource().translate((Integer) originalPage.getId(), "ina", false, null);
		ContentNodeRESTUtils.assertResponseOK(translateResponse);
		t.commit(false);
		translatedPage = t.getObject(Page.class, translateResponse.getPage().getId(), true);
		translatedPage.publish();
		if (translationLocalization != null) {
			createLocalizations(translatedPage, Arrays.asList(translationLocalization));
		}
		// Create changeable object
		Page testPage = translatedPage;
		if (fixed.getChangeableChannel() != 0) {
			testPage = createLocalizations(translatedPage, Arrays.asList(fixed.getChangeableChannel())).get(0);
		}
		// Publish
		t.commit(false);
		context.getContext().publish(false, true);
		t.commit(false);
		assertCR();

		// Take offline
		testPage.takeOffline();
		t.commit(false);
		try {
			assertDirting(true);
		} catch (Error e) {
			dumpData();
			throw e;
		}

		// Publish
		context.getContext().publish(false, true);
		t.commit(false);
		assertCR();

		// Take online
		testPage.publish();
		t.commit(false);
		assertDirting(false);

		// Publish
		context.getContext().publish(false, true);
		t.commit(false);
		assertCR();
	}

	/**
	 * Dumps data to stdout, useful for debugging test failures
	 * @throws NodeException
	 */
	private void dumpData() throws NodeException {
		DBUtils.executeStatement(
				"select id, name, contentset_id, contentgroup_id, channelset_id, channel_id, is_master, online from page where contentset_id = ?",
				new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, (Integer) originalPage.getContentsetId());
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							System.out.println(String.format("%s (%s): Channelset: %s Channel: %s (Master: %s) cset/group: %s/%s, Online: %s",
									rs.getString("name"), rs.getInt("id"), rs.getInt("channelset_id"), rs.getInt("channel_id"), rs.getInt("is_master"),
									rs.getInt("contentset_id"), rs.getInt("contentgroup_id"), rs.getInt("online")));
						}
					}
				});
		for (Node n : nodes) {
			System.out.println("\n" + n);
			try {
				DBHandle x = n.getContentMap().getHandle();
				PoolConnection y = DB.getPoolConnection(x);
				Connection z = y.getConnection();

				PreparedStatement stmt = z.prepareStatement("select * from contentattribute where contentid like '10007.%' and name like 'contentid_%'");
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					System.out.printf("%s\t%s\t%s\n", rs.getString(2), rs.getString(3), rs.getString(4));
				}
				rs.close();
				stmt.close();
				z.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NodeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
			}
		}
	}

	/**
	 * Localize/create or unlocalize/delete a page (depending on the position in the channel tree)
	 * @throws Exception
	 */
	@Test
	public void testAddRemove() throws Exception {
		// We can't localize something if its master object is not yet created
		if (fixed.getChangeableChannel() == 0 && translationLocalization != null) {
			return;
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		// Create fixed contentset
		originalPage = Creator.createPage(getName(), nodes[0].getFolder(), template, klingon);
		createLocalizations(originalPage, fixed.getChannelsWithLocalizations());
		originalPage.publish();
		t.commit(false);

		// Create change-contentset
		PageLoadResponse translateResponse = getPageResource().translate((Integer) originalPage.getId(), "ina", false, null);
		ContentNodeRESTUtils.assertResponseOK(translateResponse);
		translatedPage = t.getObject(Page.class, translateResponse.getPage().getId(), true);
		if (fixed.getChangeableChannel() != 0) {
			translatedPage.publish();
		}
		if (translationLocalization != null) {
			createLocalizations(translatedPage, Arrays.asList(translationLocalization));
		}

		t.commit(false);
		context.getContext().publish(false, true);
		t.commit(false);
		assertCR();

		Page testPage = null;

		// Create changeable object
		if (fixed.getChangeableChannel() == 0) {
			translatedPage.publish();
			testPage = translatedPage;
		} else {
			testPage = createLocalizations(translatedPage, Arrays.asList(fixed.getChangeableChannel())).get(0);
		}
		t.commit(false);

		assertDirting(false);

		// Publish
		context.getContext().publish(false, true);
		t.commit(false);
		assertCR();

		// Remove changeable object
		t.setChannelId(nodes[fixed.getChangeableChannel()].getId());
		try {
			testPage = t.getObject(Page.class, testPage.getId());
		} finally {
			t.resetChannel();
		}
		testPage.delete();
		t.commit(false);
		assertDirting(true);

		// Publish
		context.getContext().publish(false, true);
		t.commit(false);
		assertCR();
	}

	/**
	 * Create localizations
	 * @param pageToLocalize create localizations from this page
	 * @param localizationIndices list of n; localizations are created in channel this.nodes[n]
	 * @return list of newly created localizations in order of localizationIndices
	 * @throws NodeException
	 */
	private List<Page> createLocalizations(Page pageToLocalize, List<Integer> localizationIndices) throws NodeException {
		List<Page> result = new ArrayList<Page>();
		for (Integer channelIndex : localizationIndices) {
			Page localized = (Page) pageToLocalize.copy();
			localized.setChannelInfo(nodes[channelIndex].getId(), pageToLocalize.getChannelSetId());
			localized.save();
			localized.publish();
			result.add(localized);
		}
		return result;
	}

	/**
	 * Check the publish queue against a diff of the node objects and the objects from the content repository
	 * @param strict if true, only pages with actual changes in the languageset are allowed in the publishqueue
	 * @throws Exception
	 */
	private void assertDirting(boolean strict) throws Exception {
		context.getContext().startDirtQueueWorker();
		context.getContext().waitForDirtqueueWorker(context.getDBSQLUtils());
		Transaction t = TransactionManager.getCurrentTransaction();
		for (Node c : nodes) {
			t.setChannelId(c.getId());
			try {
				// Get Info from CR
				Page channelPage = t.getObject(Page.class, originalPage.getId());
				Integer cRReferencedId = getReferencedPageIDFromCR(channelPage, c, "contentid_ina");

				// Compare to Node objects
				Page channelTranslation = channelPage.getLanguageVariant("ina", c.getId());
				if (channelTranslation != null && !channelTranslation.isOnline()) {
					channelTranslation = null;
				}

				boolean bothNull = (cRReferencedId == null && channelTranslation == null);
				boolean bothIdentical = cRReferencedId != null && channelTranslation != null && cRReferencedId.equals(channelTranslation.getId());
				boolean changed = !bothNull && !bothIdentical;
				List<Integer> dirtedObjectIds = PublishQueue.getDirtedObjectIds(Page.class, false, c);
				if (changed) {
					assertTrue("There must be a publish queue entry for the source page " + channelPage.getId() + " in channel " + c,
							dirtedObjectIds.contains(channelPage.getId()));
				} else if (strict) {
					assertFalse("Extraneous publish queue entry for the source page " + channelPage.getId() + " in channel " + c,
							dirtedObjectIds.contains(channelPage.getId()));
				}
			} finally {
				t.resetChannel();
			}
		}
	}

	/**
	 * Reads a page reference from an attribute of a page in the CR. Fails if the reference is to a page not existing in the CR. Returns null if no reference found.
	 * @param sourcePage the page where to look for the reference
	 * @param cRNode the node to obtain the objects from
	 * @param attributeName
	 * @return
	 * @throws NodeException
	 */
	private Integer getReferencedPageIDFromCR(Page sourcePage, Node cRNode, String attributeName) throws NodeException {
		WriteableDatasource ds = cRNode.getContentMap().getDatasource();
		if (ds == null) {
			WritableMCCRDatasource mccrDS = cRNode.getContentMap().getMCCRDatasource();
			mccrDS.setChannel((Integer) cRNode.getId());
			ds = mccrDS;
		}

		String containeroid = "10007." + sourcePage.getId();

		// Filter for pages with nonexisting referenced objects
		String noReferenceFilter = "object.contentid == '" + containeroid + "' AND isempty(object." + attributeName + ")";
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(noReferenceFilter));
		Collection<Resolvable> result = ds.getResult(filter, null);
		if (!result.isEmpty()) {
			return null;
		}
		Resolvable pageObject = PortalConnectorFactory.getContentObject(containeroid, ds);
		Object referencedObject = pageObject.get(attributeName);
		if (!(referencedObject instanceof Resolvable)) {
			fail("Stale reference in contenrepository detected in page " + containeroid);
			// should never happen
			return null;
		} else {
			Resolvable referencedPage = (Resolvable) referencedObject;
			String contentId = (String) referencedPage.get("contentid");
			if (!contentId.startsWith("10007.")) {
				fail("Unknown object referenced in page " + containeroid);
				// should never happen
				return null;
			} else {
				return Integer.parseInt(contentId.substring(6));
			}
		}
	}

	/**
	 * Check if the state of the CR is in line with the node objects
	 * @throws NodeException
	 */
	private void assertCR() throws NodeException {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (Node c : nodes) {
				t.setChannelId(c.getId());
				try {
					Page channelPage = t.getObject(Page.class, originalPage.getId());
					Page translatedChannelPage = channelPage.getLanguageVariant("ina");
					if (translatedChannelPage != null) {
						if (!translatedChannelPage.isOnline() || !MultichannellingFactory.isVisibleInNode(c, translatedChannelPage)) {
							translatedChannelPage = null;
						} else {
							assertPageIdInAttribute(translatedChannelPage, translatedChannelPage, "contentid_ina");
							assertPageIdInAttribute(channelPage, translatedChannelPage, "contentid_tlh");
						}
					}
					assertPageIdInAttribute(translatedChannelPage, channelPage, "contentid_ina");
					assertPageIdInAttribute(channelPage, channelPage, "contentid_tlh");
				} finally {
					t.resetChannel();
				}
			}
		} catch (Error e) {
			dumpData();
			throw e;
		}
	}

	/**
	 * Assert that a certain page or no one is referenced by a specified attribute of a page from a CR
	 * @param referencedPage the page that should be referenced or null for no reference
	 * @param containingPage the page containing the reference
	 * @param attribute the attribute to be checked
	 * @throws DatasourceNotAvailableException
	 * @throws NodeException
	 */
	private void assertPageIdInAttribute(Page referencedPage, Page containingPage, String attribute) throws DatasourceNotAvailableException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node n = t.getObject(Node.class, nodes[0].getId());

		Datasource ds = n.getContentMap().getDatasource();
		if (ds == null) {
			WritableMCCRDatasource mccrDS = n.getContentMap().getMCCRDatasource();
			mccrDS.setChannel((Integer) n.getId());
			ds = mccrDS;
		}

		String containeroid = "10007." + containingPage.getId();
		String referenceoid = referencedPage == null ? null : ("10007." + referencedPage.getId());
		// Filter for objects with nonexisting referenced objects – must assert for empty
		String nonExistingFilter = "object.contentid == '" + containeroid + "' AND !isempty(object." + attribute + ")";
		// Filter for objects with existent referenced objects – must assert for nonempty
		String existingFilter = "object.contentid == '" + containeroid + "' AND object." + attribute + " == '" + referenceoid + "'";
		String filterString = referencedPage == null ? nonExistingFilter : existingFilter;
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(filterString));
		Collection<Resolvable> result = ds.getResult(filter, null);
		if (referencedPage == null) {
			assertTrue("Found stale reference using filter " + filterString, result.isEmpty());
		} else {
			assertTrue("Reference not found using filter " + filterString, !result.isEmpty());
		}
	}

	/**
	 * Adjust the node's contentrepository settings (e.g. adds language information)
	 * @param n adjust settings for this node
	 * @param useMastersCR iff true, set up the node to use the CR from nodes[0]
	 * @throws NodeException
	 */
	private static void adjustContentMap(Node n, boolean useMastersCR) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (useMastersCR) {
			n = t.getObject(Node.class, n.getId(), true, false, false);
			n.setContentrepositoryId(nodes[0].getContentrepositoryId());
			n.setPublishContentmap(true);
			n.save();
			t.dirtObjectCache(Node.class, n.getId());
		}
		ContentMap contentMap = n.getContentMap();
		ContentRepository cr = t.getObject(ContentRepository.class, contentMap.getId(), true);
		cr.setLanguageInformation(true);
		cr.save();

		t.commit(false);
	}

}
