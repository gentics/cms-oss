package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for status flags of pages
 */
public class PageModifiedFlagTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Template template;
	private static SystemUser user;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));
		user = supply(t -> {
			return t.getObject(SystemUser.class, 2);
		});
	}

	@Test
	public void testNew() throws NodeException {
		Page page = create(1);
		assertThat(page).as("New page").isNotModified().isOffline().isNotPlanned().isNotQueued();
	}

	@Test
	public void testNewModified() throws NodeException {
		Page page = create(1);
		page = modify(page, 2);
		assertThat(page).as("Modified new page").isNotModified().isOffline().isNotPlanned().isNotQueued();
	}

	@Test
	public void testPublished() throws NodeException {
		Page page = create(1);
		page = publish(page, 2);
		assertThat(page).as("Published page").isNotModified().isOnline().isNotPlanned().isNotQueued();
	}

	@Test
	public void testPublishedModified() throws NodeException {
		Page page = create(1);
		page = publish(page, 2);
		page = modify(page, 3);
		assertThat(page).as("Published modified page").isModified().isOnline().isNotPlanned().isNotQueued();
	}

	@Test
	public void testPlanned() throws NodeException {
		Page page = create(1);
		page = publish(page, 2);
		page = modify(page, 3);
		page = publish(page, 4, 1000);
		assertThat(page).as("Planned page").isNotModified().isOnline().isPlanned().isNotQueued();
	}

	@Test
	public void testQueued() throws NodeException {
		Page page = create(1);
		page = publish(page, 2);
		page = modify(page, 3);
		page = queue(page, 4);
		assertThat(page).as("Queued page").isModified().isOnline().isNotPlanned().isQueued();
	}

	@Test
	public void testQueuedPlanned() throws NodeException {
		Page page = create(1);
		page = publish(page, 2);
		page = modify(page, 3);
		page = queue(page, 4, 1000);
		assertThat(page).as("Queued planned page").isModified().isOnline().isNotPlanned().isQueued();
	}

	@Test
	public void testQueuedModified() throws NodeException {
		Page page = create(1);
		page = publish(page, 2);
		page = modify(page, 3);
		page = queue(page, 4);
		assertThat(page).as("Queued page").isModified().isOnline().isNotPlanned().isQueued();
		page = modify(page, 5);
		assertThat(page).as("Queued page after modification").isModified().isOnline().isNotPlanned().isNotQueued();
	}

	@Test
	public void testQueuedPlannedModified() throws NodeException {
		Page page = create(1);
		page = publish(page, 2);
		page = modify(page, 3);
		page = queue(page, 4, 1000);
		assertThat(page).as("Queued planned page").isModified().isOnline().isNotPlanned().isQueued();
		page = modify(page, 5);
		assertThat(page).as("Queued planned page after modification").isModified().isOnline().isNotPlanned().isNotQueued();
	}

	@Test
	public void testOffline() throws NodeException {
		Page page = create(1);
		page = publish(page, 2);
		page = offline(page, 3);
		assertThat(page).as("Offline page").isNotModified().isOffline().isNotPlanned().isNotQueued();
	}

	@Test
	public void testOfflineModified() throws NodeException {
		Page page = create(1);
		page = publish(page, 2);
		page = offline(page, 3);
		page = modify(page, 4);
		assertThat(page).as("Offline modified page").isModified().isOffline().isNotPlanned().isNotQueued();
	}

	@Test
	public void testCancel() throws NodeException {
		Page page = create(1);
		page = publish(page, 2, 1000);
		page = cancel(page);
		assertThat(page).as("Offline cancelled page").isNotModified().isOffline().isPlanned().isNotQueued();
	}

	/**
	 * Create page at given timestamp
	 * @param timestamp timestamp
	 * @return new page
	 * @throws NodeException
	 */
	protected Page create(int timestamp) throws NodeException {
		Page newPage = supply(t -> {
			t.setTimestamp(timestamp * 1000L);
			return createPage(node.getFolder(), template, "Page");
		});
		return supply(t -> t.getObject(newPage));
	}

	/**
	 * Modify the page
	 * @param page page
	 * @param timestamp timestamp
	 * @return modified page
	 * @throws NodeException
	 */
	protected Page modify(Page page, int timestamp) throws NodeException {
		operate(t -> {
			t.setTimestamp(timestamp * 1000L);
			Page upd = t.getObject(page, true);
			upd.setName("Modified " + upd.getName());
			upd.save();
		});
		return supply(t -> t.getObject(page));
	}

	/**
	 * Publish the page
	 * @param page page
	 * @param timestamp timestamp
	 * @return published page
	 * @throws NodeException
	 */
	protected Page publish(Page page, int timestamp) throws NodeException {
		operate(t -> {
			t.setTimestamp(timestamp * 1000L);
			t.getObject(page, true).publish();
		});
		return supply(t -> t.getObject(page));
	}

	/**
	 * Publish the page at a timestamp
	 * @param page page
	 * @param timestamp transaction timestamp
	 * @param at publish timestamp
	 * @return planned page
	 * @throws NodeException
	 */
	protected Page publish(Page page, int timestamp, int at) throws NodeException {
		operate(t -> {
			t.setTimestamp(timestamp * 1000L);
			t.getObject(page, true).publish(at, null);
		});
		return supply(t -> t.getObject(page));
	}

	/**
	 * Queue the page to be published
	 * @param page page
	 * @param timestamp timestamp
	 * @return queued page
	 * @throws NodeException
	 */
	protected Page queue(Page page, int timestamp) throws NodeException {
		operate(t -> {
			t.setTimestamp(timestamp * 1000L);
			t.getObject(page, true).queuePublish(user);
		});
		return supply(t -> t.getObject(page));
	}

	/**
	 * Queue the page to be published at a timestamp
	 * @param page page
	 * @param timestamp transaction timestamp
	 * @param at publish at timestamp
	 * @return queued page
	 * @throws NodeException
	 */
	protected Page queue(Page page, int timestamp, int at) throws NodeException {
		operate(t -> {
			t.setTimestamp(timestamp * 1000L);
			t.getObject(page, true).queuePublish(user, at, null);
		});
		return supply(t -> t.getObject(page));
	}

	/**
	 * Take the page offline
	 * @param page page
	 * @param timestamp timestamp
	 * @return offline page
	 * @throws NodeException
	 */
	protected Page offline(Page page, int timestamp) throws NodeException {
		operate(t -> {
			t.setTimestamp(timestamp * 1000L);
			t.getObject(page, true).takeOffline();
		});
		return supply(t -> t.getObject(page));
	}

	/**
	 * Take the page offline at a timestamp
	 * @param page page
	 * @param timestamp transaction timestamp
	 * @param at offline timestamp
	 * @return planned page
	 * @throws NodeException
	 */
	protected Page offline(Page page, int timestamp, int at) throws NodeException {
		operate(t -> {
			t.setTimestamp(timestamp * 1000L);
			t.getObject(page, true).takeOffline(at);
		});
		return supply(t -> t.getObject(page));
	}

	/**
	 * Cancel editing the page (restore the last version)
	 * @param page page
	 * @return page after cancelling
	 * @throws NodeException
	 */
	protected Page cancel(Page page) throws NodeException {
		operate(t -> {
			// get the last version of the page
			NodeObjectVersion[] pageVersions = page.getVersions();
			if (!ObjectTransformer.isEmpty(pageVersions)) {
				Page upd = t.getObject(page, true);
				// restore the last page version
				upd.restoreVersion(pageVersions[0], true);
				upd.unlock();
			}
		});
		return supply(t -> t.getObject(page));
	}
}
