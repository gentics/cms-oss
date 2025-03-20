package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.factory.MultichannellingFactory.getChannelVariant;
import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.job.LocalizePageCallable;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for localizing pages
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class LocalizePageTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		template = supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	@Before
	public void clean() throws NodeException {
		operate(() -> {
			clear(channel);
			clear(node);
		});
	}

	/**
	 * Test localizing an offline page
	 * @throws NodeException
	 */
	@Test
	public void testOffline() throws NodeException {
		Page masterPage = create(1);

		Page localizedCopy = localize(masterPage, 2);

		consume(m -> assertThat(localizedCopy).as("Localized Copy").isNotEqualTo(m).isOffline().isNotModified()
				.hasVersions(new NodeObjectVersion().setNumber("0.1").setCurrent(true).setPublished(false).setDate(2)),
				masterPage);
	}

	/**
	 * Test localizing a published page
	 * @throws NodeException
	 */
	@Test
	public void testPublished() throws NodeException {
		Page masterPage = create(1);
		masterPage = publish(masterPage, 2);

		Page localizedCopy = localize(masterPage, 3);

		consume(m -> assertThat(localizedCopy).as("Localized Copy").isNotEqualTo(m).isOnline().isNotModified()
				.hasVersions(new NodeObjectVersion().setNumber("1.0").setCurrent(true).setPublished(true).setDate(3)),
				masterPage);
	}

	/**
	 * Test localizing a published and modified page
	 * @throws NodeException
	 */
	@Test
	public void testPublishedAndModified() throws NodeException {
		Page masterPage = create(1);
		masterPage = publish(masterPage, 2);
		masterPage = modify(masterPage, 3);

		Page localizedCopy = localize(masterPage, 4);

		consume(m -> assertThat(localizedCopy).as("Localized Copy").isNotEqualTo(m).isOnline().isModified().hasVersions(
				new NodeObjectVersion().setNumber("1.1").setCurrent(true).setPublished(false).setDate(5),
				new NodeObjectVersion().setNumber("1.0").setCurrent(false).setPublished(true).setDate(4)), masterPage);
	}

	/**
	 * Create a page at the given timestamp
	 * @param timestamp creation timestamp
	 * @return page
	 * @throws NodeException
	 */
	protected Page create(int timestamp) throws NodeException {
		return supply(t -> {
			t.setTimestamp(timestamp * 1000L);
			return createPage(node.getFolder(), template, "Master");
		});
	}

	/**
	 * Publish the page at the given timestamp
	 * @param page page
	 * @param timestamp timestamp
	 * @return published page
	 * @throws NodeException
	 */
	protected Page publish(Page page, int timestamp) throws NodeException {
		return execute(p -> {
			TransactionManager.getCurrentTransaction().setTimestamp(timestamp * 1000L);
			return update(p, upd -> upd.publish());
		}, page);
	}

	/**
	 * Modify the page at the given timestamp
	 * @param page page
	 * @param timestamp timestamp
	 * @return modified page
	 * @throws NodeException
	 */
	protected Page modify(Page page, int timestamp) throws NodeException {
		return execute(p -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setTimestamp(timestamp * 1000L);
			Page editable = t.getObject(p ,true);
			editable.setName(editable.getName() + " Modified");
			editable.save();
			editable.unlock();
			return editable.reload();
		}, page);
	}

	/**
	 * Localize the page at the given timestamp
	 * @param page page
	 * @param timestamp timestamp
	 * @return localized copy
	 * @throws NodeException
	 */
	protected Page localize(Page page, int timestamp) throws NodeException {
		operate(t -> {
			t.setTimestamp(timestamp * 1000L);
			LocalizePageCallable callable = new LocalizePageCallable(page.getId(), channel.getId(), true);
			try {
				assertResponseOK(callable.call());
			} catch (NodeException e) {
				throw e;
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});

		return supply(() -> getChannelVariant(page, channel));
	}
}
