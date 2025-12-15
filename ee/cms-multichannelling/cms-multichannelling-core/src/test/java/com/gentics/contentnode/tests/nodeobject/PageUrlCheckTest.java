package com.gentics.contentnode.tests.nodeobject;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.TestedInheritance;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Test cases for checking invalid page URLs
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.INVALIDPAGEURLMSG })
@RunWith(value = Parameterized.class)
public class PageUrlCheckTest {
	/**
	 * Key of the i18n message for invalid links
	 */
	public final static String INVALID_LINK_MSG = "notification.linkfrompage.invalid";

	/**
	 * Key of the i18n message (part) for deleted pages
	 */
	public final static String DELETED_MSG = "deleted_lc";

	/**
	 * Key of the i18n message (part) for page taken offline
	 */
	public final static String OFFLINE_MSG = "taken offline";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node master;

	private static Node channel;

	private static int linkConstructId;

	@Parameters(name = "{index}: source {0}, target {1}, remove in {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedInheritance source : TestedInheritance.values()) {
			for (TestedInheritance target : TestedInheritance.values()) {
				for (HandledNode removeTargetIn : HandledNode.values()) {
					if (removeTargetIn.possibleFor(target)) {
						data.add(new Object[] { source, target, removeTargetIn });
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		try (Trx trx = new Trx()) {
			master = ContentNodeTestDataUtils.createNode("master", "Master", PublishTarget.NONE);
			channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "channel", "/");
			linkConstructId = ContentNodeTestDataUtils.createConstruct(master, PageURLPartType.class, "link", "url");
			trx.success();
		}
	}

	@Parameter(0)
	public TestedInheritance source;

	@Parameter(1)
	public TestedInheritance target;

	@Parameter(2)
	public HandledNode removeTargetIn;

	private LocalizableNodeObject<?>[] sourcePages;

	private LocalizableNodeObject<?>[] targetPages;

	private Page targetMaster;

	private String tagName;

	@Before
	public void setup() throws Exception {
		try (Trx trx = new Trx()) {
			sourcePages = source.createPublished(MCTestedType.page, master, channel);
			targetPages = target.createPublished(MCTestedType.page, master, channel);

			// determine the master page of the link targets
			if (targetPages[0] instanceof Page) {
				targetMaster = (Page)targetPages[0];
			} else if (targetPages[1] instanceof Page) {
				targetMaster = (Page)targetPages[1];
			}

			// let the source page(s) link to the target
			for (int i = 0; i < 2; i++) {
				if (sourcePages[i] instanceof Page) {
					Page editable = trx.getTransaction().getObject(Page.class, sourcePages[i].getId(), true);
					ContentTag linkTag = editable.getContent().addContentTag(linkConstructId);
					linkTag.setName("link1");
					tagName = linkTag.getName();
					ContentNodeTestDataUtils.getPartType(PageURLPartType.class, linkTag, "url").setTargetPage(targetMaster);
					editable.save();
					editable.publish();

					sourcePages[i] = trx.getTransaction().getObject(sourcePages[i]);
				}
			}

			// delete all inbox messages
			DBUtils.executeStatement("DELETE FROM msg", Transaction.UPDATE_STATEMENT);

			trx.success();
		}
	}

	@Test
	public void testTakeOffline() throws Exception {
		// take the target offline
		try (Trx trx = new Trx()) {
			trx.getTransaction().getObject(getHandledPage(), true, false).takeOffline();
			trx.success();
		}

		assertExpectedMessages(false);
	}

	@Test
	public void testTakeOfflineNoFeature() throws Exception {
		// take the target offline
		try (FeatureClosure noMsg = new FeatureClosure(Feature.INVALIDPAGEURLMSG, false); Trx trx = new Trx()) {
			trx.getTransaction().getObject(getHandledPage(), true, false).takeOffline();
			trx.success();
		}

		assertNoMessages();
	}

	@Test
	public void testDelete() throws Exception {
		// delete the target
		try (Trx trx = new Trx()) {
			trx.getTransaction().getObject(getHandledPage(), true, false).delete();
			trx.success();
		}

		assertExpectedMessages(true);
	}

	@Test
	public void testTakeOfflineBoth() throws Exception {
		// Take offline the source page and the target page
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			if (sourcePages[1] != null) {
				t.getObject((Page)sourcePages[1], true, false).takeOffline();
			}

			if (sourcePages[0] != null) {
				t.getObject((Page)sourcePages[0], true, false).takeOffline();
			}

			trx.success();
		}

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			t.getObject(getHandledPage(), true, false).takeOffline();
			trx.success();
		}

		// No message should be generated
		assertExpectedMessages(true);
	}

	@Test
	public void testDeleteNoFeature() throws Exception {
		// delete the target
		try (FeatureClosure noMsg = new FeatureClosure(Feature.INVALIDPAGEURLMSG, false); Trx trx = new Trx()) {
			trx.getTransaction().getObject(getHandledPage(), true, false).delete();
			trx.success();
		}

		assertNoMessages();
	}

	@Test
	public void testDeleteIntoWastebin() throws Exception {
		// delete the target
		try (FeatureClosure wastebin = new FeatureClosure(Feature.WASTEBIN, true); Trx trx = new Trx()) {
			trx.getTransaction().getObject(getHandledPage(), true, false).delete();
			trx.success();
		}

		assertExpectedMessages(true);
	}

	/**
	 * Deleting a page from the wastebin should not generate a message
	 * @throws Exception
	 */
	@Test
	public void testDeleteFromWastebin() throws Exception {
		Page handledPage = null;

		try (FeatureClosure wastebin = new FeatureClosure(Feature.WASTEBIN, true)) {
			// delete the target
			try (Trx trx = new Trx()) {
				handledPage = trx.getTransaction().getObject(getHandledPage(), true, false);
				handledPage.delete();
				trx.success();
			}
	
			assertExpectedMessages(true);
	
			try (Trx trx = new Trx()) {
				DBUtils.executeStatement("DELETE FROM msg", Transaction.DELETE_STATEMENT);
				trx.success();
			}
	
			try (Trx trx = new Trx(); WastebinFilter filter = Wastebin.INCLUDE.set()) {
				handledPage = trx.getTransaction().getObject(handledPage, true, false);
				handledPage.delete(true);
				trx.success();
			}
		}

		assertNoMessages();
	}

	@Test
	public void testDeleteIntoWastebinNoFeature() throws Exception {
		// delete the target
		try (FeatureClosure noMsg = new FeatureClosure(Feature.INVALIDPAGEURLMSG, false);
				FeatureClosure wastebin = new FeatureClosure(Feature.WASTEBIN, true);
				Trx trx = new Trx()) {
			trx.getTransaction().getObject(getHandledPage(), true, false).delete();
			trx.success();
		}

		assertNoMessages();
	}

	/**
	 * Assert that no messages were sent
	 * @throws NodeException
	 */
	protected void assertNoMessages() throws NodeException {
		try (Trx trx = new Trx()) {
			DBUtils.executeStatement("SELECT * FROM msg", Transaction.SELECT_STATEMENT, null, rs -> {
				while (rs.next()) {
					fail("Found unexpected message " + rs.getString("msg"));
				}
			});
			trx.success();
		}
	}

	/**
	 * Assert that exactly the expected messages were sent
	 * @param deleted true if the target page was deleted, false if taken offline
	 * @throws NodeException
	 */
	protected void assertExpectedMessages(boolean deleted) throws NodeException {
		try (Trx trx = new Trx()) {
			Set<String> expectedMessages = getExpectedMessages(deleted);

			DBUtils.executeStatement("SELECT * FROM msg", Transaction.SELECT_STATEMENT, null, rs -> {
				while (rs.next()) {
					String message = rs.getString("msg");
					if (!expectedMessages.contains(message)) {
						fail("Found unexpected message " + message);
					}
					expectedMessages.remove(message);
				}
			});

			if (!expectedMessages.isEmpty()) {
				fail("Did not find expected messages: " + expectedMessages);
			}

			trx.success();
		}
	}

	/**
	 * Get the set of expected messages
	 * @param deleted true if target page was deleted, false if taken offline
	 * @return set of expected messages
	 * @throws NodeException
	 */
	protected Set<String> getExpectedMessages(boolean deleted) throws NodeException {
		Set<String> expectedMessages = new HashSet<>();

		// If there are no source pages, no messages are expected
		if ((sourcePages[0] == null || !((Page)sourcePages[0]).isOnline())
				&& (sourcePages[1] == null || !((Page)sourcePages[1]).isOnline())) {
			return expectedMessages;
		}

		switch (source) {
		case inherited:
			switch (target) {
			case inherited:
				// normal case
				expectedMessages.add(composeMessage((Page) sourcePages[0], (Page) targetPages[0], deleted));
				break;
			case localized:
				switch (removeTargetIn) {
				case master:
					// target removed in master, so link in master is invalid
					expectedMessages.add(composeMessage((Page) sourcePages[0], (Page) targetPages[0], deleted));
					break;
				case channel:
					// target removed only in channel, link in master is still valid
					break;
				}
				break;
			case local:
				// link in master page was always invalid
				break;
			}
			break;
		case localized:
			switch (target) {
			case inherited:
				// both master and channel page linked to the same page
				expectedMessages.add(composeMessage((Page)sourcePages[0], (Page)targetPages[0], deleted));
				expectedMessages.add(composeMessage((Page)sourcePages[1], (Page)targetPages[0], deleted));
				break;
			case localized:
				switch (removeTargetIn) {
				case master:
					// only link in master is invalid now
					expectedMessages.add(composeMessage((Page)sourcePages[0], (Page)targetPages[0], deleted));
					if (deleted) {
						// when the master was deleted, so was the channel object
						expectedMessages.add(composeMessage((Page)sourcePages[1], (Page)targetPages[0], deleted));
					}
					break;
				case channel:
					// only link in channel is invalid now
//					expectedMessages.add(composeMessage((Page)sourcePages[1], (Page)targetPages[1], deleted));
					break;
				}
				break;
			case local:
				// only link in channel is invalid (link in master was invalid before)
				expectedMessages.add(composeMessage((Page)sourcePages[1], (Page)targetPages[1], deleted));
				break;
			}
			break;
		case local:
			switch (target) {
			case inherited:
				// link in channel is invalid now
				expectedMessages.add(composeMessage((Page)sourcePages[1], (Page)targetPages[0], deleted));
				break;
			case localized:
				switch (removeTargetIn) {
				case master:
					// link in channel still links to localized copy
					if (deleted) {
						// when the master was deleted, so was the channel object
						expectedMessages.add(composeMessage((Page)sourcePages[1], (Page)targetPages[0], deleted));
					}
					break;
				case channel:
					// link in channel now links to removed/offline localized copy
//					expectedMessages.add(composeMessage((Page)sourcePages[1], (Page)targetPages[1], deleted));
					break;
				}
				break;
			case local:
				// normal case in channel
				expectedMessages.add(composeMessage((Page)sourcePages[1], (Page)targetPages[1], deleted));
				break;
			}
			break;
		}

		return expectedMessages;
	}

	/**
	 * Compose the expected message
	 * @param sourcePage source page
	 * @param targetPage target page
	 * @param deleted true if the source page was deleted, false if taken offline
	 * @return message
	 * @throws NodeException
	 */
	protected String composeMessage(Page sourcePage, Page targetPage, boolean deleted) throws NodeException {
		CNI18nString message = new CNI18nString(INVALID_LINK_MSG);

		String linkQualifiedName = "";
		String targetQualifiedName = "";

		try (ChannelTrx trx = new ChannelTrx(sourcePage.getChannel())) {
			linkQualifiedName = sourcePage.getQualifiedName();
		}

		try (ChannelTrx trx = new ChannelTrx(targetPage.getChannel())) {
			targetQualifiedName = targetPage.getQualifiedName();
		}

		CNI18nString action = new CNI18nString(deleted ? DELETED_MSG : OFFLINE_MSG);

		message.addParameters(new String[] {
			tagName,
			linkQualifiedName,
			String.valueOf(sourcePage.getId()),
			targetQualifiedName,
			String.valueOf(targetPage.getId()),
			action.toString()
		});

		return message.toString();
	}

	/**
	 * Get the handled page
	 * @return handled page
	 */
	protected Page getHandledPage() {
		switch (removeTargetIn) {
		case master:
			return (Page)targetPages[0];
		case channel:
			return (Page)targetPages[1];
		default:
			return null;
		}
	}

	/**
	 * Enum for node, in which the target page is removed
	 */
	protected static enum HandledNode {
		master,
		channel;

		/**
		 * Check whether it is possible to remove the target page in this node,
		 * when the target page has given inheritance
		 * @param inheritance inheritance
		 * @return true iff removing is possible
		 */
		public boolean possibleFor(TestedInheritance inheritance) {
			switch (inheritance) {
			case inherited:
				return this == master;
			case localized:
				return true;
			case local:
				return this == channel;
			default:
				return false;
			}
		}
	}
}
