package com.gentics.contentnode.tests.timemanagement;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createForm;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the publisher (unpublisher) recorded when a form was scheduled for publishing ("publish at")
 * or taken offline by one user and later published (taken offline) by another user (SUP-20441)
 */
public class FormFuturePublisherTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	/**
	 * User who schedules the publishing (or takes the form offline first)
	 */
	private static SystemUser userA;

	/**
	 * User who publishes (takes offline) afterwards
	 */
	private static SystemUser userB;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());

		UserGroup group = supply(() -> createUserGroup("Form Publishers", NODE_GROUP_ID));
		userA = supply(() -> createSystemUser("Form User", "A", null, "formusera", "formusera", Arrays.asList(group)));
		userB = supply(() -> createSystemUser("Form User", "B", null, "formuserb", "formuserb", Arrays.asList(group)));
	}

	/**
	 * User A schedules the form for publishing, the scheduled publishing is executed by the time management,
	 * afterwards user B publishes the form. B must be the publisher.
	 * @throws NodeException
	 */
	@Test
	public void testPublishAfterScheduledPublish() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int publishAt = now + 3600;

		Form form = createFormAs(userA, now);
		publishAs(userA, form, now + 1, publishAt);

		runTimeManagement(form, publishAt + 1);

		form = execute(Form::reload, form);
		consume(f -> {
			assertThat(f.isOnline()).as("Form online after scheduled publish").isTrue();
			assertThat(f.getPublisher().getId()).as("Publisher after scheduled publish").isEqualTo(userA.getId());
		}, form);

		publishAs(userB, form, publishAt + 2, 0);

		assertPublisher(form, userB);
	}

	/**
	 * User A schedules the form for publishing, user B publishes the form before the scheduled date.
	 * B must be the publisher.
	 * @throws NodeException
	 */
	@Test
	public void testPublishBeforeScheduledDate() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int publishAt = now + 3600;

		Form form = createFormAs(userA, now);
		publishAs(userA, form, now + 1, publishAt);
		publishAs(userB, form, now + 2, 0);

		assertPublisher(form, userB);
	}

	/**
	 * User A takes the form offline and publishes it again, afterwards user B takes the form offline.
	 * B must be the unpublisher.
	 * @throws NodeException
	 */
	@Test
	public void testTakeOfflineAfterPreviousOffline() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);

		Form form = createFormAs(userA, now);
		publishAs(userA, form, now + 1, 0);
		takeOfflineAs(userA, form, now + 2);
		publishAs(userA, form, now + 3, 0);
		takeOfflineAs(userB, form, now + 4);

		Form reloaded = execute(Form::reload, form);
		consume(f -> {
			assertThat(f.isOnline()).as("Form online after taking offline").isFalse();
			assertThat(f.getUnpublisher().getId()).as("Unpublisher after taking offline by B").isEqualTo(userB.getId());
		}, reloaded);
		assertThat(getStoredUserId("future_unpublisher", form.getId())).as("Stored future unpublisher").isEqualTo(0);
	}

	/**
	 * User B schedules the form for publishing, afterwards user A schedules the form for publishing at a time, which is already due.
	 * The form is published immediately, A must be the publisher.
	 * @throws NodeException
	 */
	@Test
	public void testScheduledPublishAlreadyDue() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int publishAt = now + 3600;

		Form form = createFormAs(userA, now);
		publishAs(userB, form, now + 1, publishAt);
		publishAs(userA, form, now + 3, now + 2);

		assertPublisher(form, userA);
	}

	/**
	 * User A schedules the form for taking offline, the scheduled offline is executed by the time management.
	 * A must be the unpublisher, the stored user must be cleared afterwards.
	 * @throws NodeException
	 */
	@Test
	public void testScheduledTakeOffline() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int offlineAt = now + 3600;

		Form form = createFormAs(userA, now);
		publishAs(userA, form, now + 1, 0);
		scheduleTakeOfflineAs(userA, form, now + 2, offlineAt);

		assertThat(getStoredUserId("future_unpublisher", form.getId())).as("Stored future unpublisher after scheduling")
				.isEqualTo(userA.getId());

		runTimeManagement(form, offlineAt + 1);

		Form reloaded = execute(Form::reload, form);
		consume(f -> {
			assertThat(f.isOnline()).as("Form online after scheduled offline").isFalse();
			assertThat(f.getUnpublisher()).as("Unpublisher after scheduled offline").isNotNull();
			assertThat(f.getUnpublisher().getId()).as("Unpublisher after scheduled offline").isEqualTo(userA.getId());
		}, reloaded);
		assertThat(getStoredUserId("future_unpublisher", form.getId())).as("Stored future unpublisher").isEqualTo(0);
	}

	/**
	 * User A schedules the form for taking offline, user B takes the form offline before the scheduled date.
	 * B must be the unpublisher.
	 * @throws NodeException
	 */
	@Test
	public void testTakeOfflineBeforeScheduledDate() throws NodeException {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int offlineAt = now + 3600;

		Form form = createFormAs(userA, now);
		publishAs(userA, form, now + 1, 0);
		scheduleTakeOfflineAs(userA, form, now + 2, offlineAt);
		takeOfflineAs(userB, form, now + 3);

		Form reloaded = execute(Form::reload, form);
		consume(f -> {
			assertThat(f.isOnline()).as("Form online after taking offline").isFalse();
			assertThat(f.getUnpublisher()).as("Unpublisher after taking offline by B").isNotNull();
			assertThat(f.getUnpublisher().getId()).as("Unpublisher after taking offline by B").isEqualTo(userB.getId());
		}, reloaded);
		assertThat(getStoredUserId("future_unpublisher", form.getId())).as("Stored future unpublisher").isEqualTo(0);
	}

	/**
	 * Assert that the form is online, has the expected publisher and no stored future publisher
	 * @param form form
	 * @param expected expected publisher
	 * @throws NodeException
	 */
	protected void assertPublisher(Form form, SystemUser expected) throws NodeException {
		Form reloaded = execute(Form::reload, form);
		consume(f -> {
			assertThat(f.isOnline()).as("Form online after manual publish").isTrue();
			assertThat(f.getPublisher().getId()).as("Publisher after manual publish by B").isEqualTo(expected.getId());
		}, reloaded);
		assertThat(getStoredUserId("future_publisher", form.getId())).as("Stored future publisher").isEqualTo(0);
	}

	/**
	 * Create a form as the given user
	 * @param user user
	 * @param timestamp transaction timestamp in seconds
	 * @return form
	 * @throws NodeException
	 */
	protected Form createFormAs(SystemUser user, int timestamp) throws NodeException {
		try (Trx trx = new Trx(user)) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			Form form = createForm(node.getFolder(), "form");
			trx.success();
			return form;
		}
	}

	/**
	 * Publish the form as the given user
	 * @param user user
	 * @param form form
	 * @param timestamp transaction timestamp in seconds
	 * @param at "publish at" timestamp, 0 to publish now
	 * @throws NodeException
	 */
	protected void publishAs(SystemUser user, Form form, int timestamp, int at) throws NodeException {
		try (Trx trx = new Trx(user)) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			trx.getTransaction().getObject(Form.class, form.getId()).publish(at, false);
			trx.success();
		}
	}

	/**
	 * Take the form offline now as the given user
	 * @param user user
	 * @param form form
	 * @param timestamp transaction timestamp in seconds
	 * @throws NodeException
	 */
	protected void takeOfflineAs(SystemUser user, Form form, int timestamp) throws NodeException {
		try (Trx trx = new Trx(user)) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			trx.getTransaction().getObject(Form.class, form.getId()).takeOffline(0);
			trx.success();
		}
	}

	/**
	 * Schedule the form for taking offline ("offline at") as the given user
	 * @param user user
	 * @param form form
	 * @param timestamp transaction timestamp in seconds
	 * @param at "offline at" timestamp
	 * @throws NodeException
	 */
	protected void scheduleTakeOfflineAs(SystemUser user, Form form, int timestamp, int at) throws NodeException {
		try (Trx trx = new Trx(user)) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			trx.getTransaction().getObject(Form.class, form.getId()).takeOffline(at);
			trx.success();
		}
	}

	/**
	 * Run the time management for the form like the publish run does (transaction without user, i.e. user ID 0)
	 * @param form form
	 * @param timestamp transaction timestamp in seconds
	 * @throws NodeException
	 */
	protected void runTimeManagement(Form form, int timestamp) throws NodeException {
		try (Trx trx = new Trx(null, 0)) {
			trx.getTransaction().setTimestamp(timestamp * 1000L);
			Form scheduled = trx.getTransaction().getObject(Form.class, form.getId());
			assertThat(scheduled.handleTimemanagement()).as("Time Management handled").isTrue();
			trx.success();
		}
	}

	/**
	 * Get the value of a stored user column of the form from the DB
	 * @param column column name (future_publisher or future_unpublisher)
	 * @param id form ID
	 * @return stored user ID
	 * @throws NodeException
	 */
	protected int getStoredUserId(String column, int id) throws NodeException {
		return supply(() -> DBUtils.select("SELECT " + column + " FROM form WHERE id = ?", ps -> ps.setInt(1, id),
				DBUtils.firstInt(column)));
	}
}
