package com.gentics.contentnode.tests.timemanagement;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.object.Node.TYPE_NODE;
import static com.gentics.contentnode.perm.PermHandler.FULL_PERM;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the publisher (unpublisher) recorded when a page was scheduled for publishing ("publish at")
 * or taking offline ("offline at") by one user and later published (taken offline) by another user (SUP-20394)
 */
public class FuturePublisherTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	/**
	 * User who schedules the publishing
	 */
	private static SystemUser userA;

	/**
	 * User who publishes manually afterwards
	 */
	private static SystemUser userB;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		UserGroup group = supply(() -> createUserGroup("Publishers", NODE_GROUP_ID));
		userA = supply(() -> createSystemUser("User", "A", null, "usera", "usera", Arrays.asList(group)));
		userB = supply(() -> createSystemUser("User", "B", null, "userb", "userb", Arrays.asList(group)));
		operate(() -> setPermissions(TYPE_NODE, node.getFolder().getId(), Arrays.asList(group), FULL_PERM));
	}

	/**
	 * Customer repro: user A schedules the page for publishing, the scheduled publishing is executed by the
	 * time management, afterwards user B publishes the page. B must be the publisher.
	 * @throws NodeException
	 */
	@Test
	public void testPublishAfterScheduledPublish() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int publishAt = now + 3600;

		Page page = createPageAs(userA, now);
		publishAs(userA, page, now + 1, publishAt);

		runTimeManagement(page, publishAt + 1);

		page = execute(Page::reload, page);
		consume(p -> {
			assertThat(p).as("Page after scheduled publish").isOnline();
			assertThat(p.getPublisher().getId()).as("Publisher after scheduled publish").isEqualTo(userA.getId());
		}, page);

		publishAs(userB, page, publishAt + 2, 0);

		page = execute(Page::reload, page);
		consume(p -> {
			assertThat(p).as("Page after manual publish").isOnline();
			assertThat(p.getPublisher().getId()).as("Publisher after manual publish by B").isEqualTo(userB.getId());
		}, page);
		assertThat(getStoredUserId("page", "future_publisher", page.getId())).as("Stored future publisher").isEqualTo(0);
	}

	/**
	 * User A schedules the page for publishing, user B publishes the page before the scheduled date.
	 * B must be the publisher.
	 * @throws NodeException
	 */
	@Test
	public void testPublishBeforeScheduledDate() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int publishAt = now + 3600;

		Page page = createPageAs(userA, now);
		publishAs(userA, page, now + 1, publishAt);
		publishAs(userB, page, now + 2, 0);

		page = execute(Page::reload, page);
		consume(p -> {
			assertThat(p).as("Page after manual publish").isOnline();
			assertThat(p.getPublisher().getId()).as("Publisher after manual publish by B").isEqualTo(userB.getId());
		}, page);
		assertThat(getStoredUserId("page", "future_publisher", page.getId())).as("Stored future publisher").isEqualTo(0);
	}

	/**
	 * User A schedules the page for taking offline, user B takes the page offline before the scheduled date.
	 * B must be the unpublisher.
	 * @throws NodeException
	 */
	@Test
	public void testTakeOfflineBeforeScheduledDate() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int offlineAt = now + 3600;

		Page page = createPageAs(userA, now);
		publishAs(userA, page, now + 1, 0);
		takeOfflineAs(userA, page, now + 2, offlineAt);
		takeOfflineAs(userB, page, now + 3, 0);

		assertUnpublisher(page, userB, "Unpublisher after manual offline by B");
	}

	/**
	 * User A schedules the page for taking offline, the scheduled offline is executed by the time management.
	 * A must be the unpublisher, the stored user must be cleared afterwards.
	 * @throws NodeException
	 */
	@Test
	public void testScheduledTakeOffline() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int offlineAt = now + 3600;

		Page page = createPageAs(userA, now);
		publishAs(userA, page, now + 1, 0);
		takeOfflineAs(userA, page, now + 2, offlineAt);

		runTimeManagement(page, offlineAt + 1);

		assertUnpublisher(page, userA, "Unpublisher after scheduled offline");
	}

	/**
	 * User A schedules the page for taking offline, the scheduled date has passed, but the time management did not run yet,
	 * when user B takes the page offline. B must be the unpublisher.
	 * @throws NodeException
	 */
	@Test
	public void testTakeOfflineAfterDueBeforeTimeManagement() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int offlineAt = now + 3600;

		Page page = createPageAs(userA, now);
		publishAs(userA, page, now + 1, 0);
		takeOfflineAs(userA, page, now + 2, offlineAt);
		takeOfflineAs(userB, page, offlineAt + 10, 0);

		assertUnpublisher(page, userB, "Unpublisher after manual offline by B");
	}

	/**
	 * User A schedules the page for taking offline, the scheduled offline is executed by the time management,
	 * afterwards the page is published again and user B takes it offline. B must be the unpublisher.
	 * @throws NodeException
	 */
	@Test
	public void testTakeOfflineAfterScheduledOffline() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int offlineAt = now + 3600;

		Page page = createPageAs(userA, now);
		publishAs(userA, page, now + 1, 0);
		takeOfflineAs(userA, page, now + 2, offlineAt);

		runTimeManagement(page, offlineAt + 1);

		publishAs(userB, page, offlineAt + 2, 0);
		takeOfflineAs(userB, page, offlineAt + 3, 0);

		assertUnpublisher(page, userB, "Unpublisher after manual offline by B");
	}

	/**
	 * Assert that the page is offline, has the expected unpublisher and no stored future unpublisher
	 * @param page page
	 * @param expected expected unpublisher
	 * @param description assertion description
	 * @throws NodeException
	 */
	protected void assertUnpublisher(Page page, SystemUser expected, String description) throws NodeException {
		Page reloaded = execute(Page::reload, page);
		consume(p -> {
			assertThat(p).as("Page after taking offline").isOffline();
			assertThat(p.getUnpublisher().getId()).as(description).isEqualTo(expected.getId());
		}, reloaded);
		assertThat(getStoredUserId("page", "future_unpublisher", page.getId())).as("Stored future unpublisher").isEqualTo(0);
	}

	/**
	 * Create a page as the given user
	 * @param user user
	 * @param timestamp transaction timestamp in seconds
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPageAs(SystemUser user, int timestamp) throws NodeException {
		try (Trx trx = new Trx(user)) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			Page page = createPage(node.getFolder(), template, "page");
			trx.success();
			return page;
		}
	}

	/**
	 * Publish the page via REST as the given user
	 * @param user user
	 * @param page page
	 * @param timestamp transaction timestamp in seconds
	 * @param at "publish at" timestamp, 0 to publish now
	 * @throws NodeException
	 */
	protected void publishAs(SystemUser user, Page page, int timestamp, int at) throws NodeException {
		try (Trx trx = new Trx(user)) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			PagePublishRequest request = new PagePublishRequest();
			if (at > 0) {
				request.setAt(at);
			}
			GenericResponse response = new PageResourceImpl().publish(String.valueOf(page.getId()), null, request);
			assertResponseCodeOk(response);
			trx.success();
		}
	}

	/**
	 * Take the page offline via REST as the given user
	 * @param user user
	 * @param page page
	 * @param timestamp transaction timestamp in seconds
	 * @param at "offline at" timestamp, 0 to take offline now
	 * @throws NodeException
	 */
	protected void takeOfflineAs(SystemUser user, Page page, int timestamp, int at) throws NodeException {
		try (Trx trx = new Trx(user)) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			PageOfflineRequest request = new PageOfflineRequest();
			if (at > 0) {
				request.setAt(at);
			}
			GenericResponse response = new PageResourceImpl().takeOffline(String.valueOf(page.getId()), request);
			assertResponseCodeOk(response);
			trx.success();
		}
	}

	/**
	 * Run the time management for the page like the publish run does (transaction without user, i.e. user ID 0)
	 * @param page page
	 * @param timestamp transaction timestamp in seconds
	 * @throws NodeException
	 */
	protected void runTimeManagement(Page page, int timestamp) throws NodeException {
		try (Trx trx = new Trx(null, 0)) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			Page scheduled = trx.getTransaction().getObject(Page.class, page.getId());
			assertThat(scheduled.handleTimemanagement()).as("Time Management handled").isTrue();
			trx.success();
		}
	}

	/**
	 * Get the value of a stored user column (future_publisher, future_unpublisher) from the DB
	 * @param table table name (page or form)
	 * @param column column name
	 * @param id object ID
	 * @return stored user ID
	 * @throws NodeException
	 */
	protected int getStoredUserId(String table, String column, int id) throws NodeException {
		return supply(() -> DBUtils.select("SELECT " + column + " FROM " + table + " WHERE id = ?", ps -> ps.setInt(1, id),
				DBUtils.firstInt(column)));
	}
}
