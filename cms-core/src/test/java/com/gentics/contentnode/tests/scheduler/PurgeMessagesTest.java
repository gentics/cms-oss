package com.gentics.contentnode.tests.scheduler;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.PropertyTrx;
import com.gentics.contentnode.scheduler.CoreInternalSchedulerTask;
import com.gentics.contentnode.scheduler.PurgeMessagesJob;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Tests for the {@link PurgeMessagesJob}
 */
public class PurgeMessagesTest {
	public final static String NOW_MESSAGE = "Message from just now";

	public final static String LAST_WEEK_MESSAGE = "Message from a week before";

	public final static String LAST_MONTH_MESSAGE = "Message from a month before";

	public final static String LAST_YEAR_MESSAGE = "Message from a year before";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
	}

	@Before
	public void setup() throws NodeException {
		// clear all messages
		operate(() -> {
			DBUtils.update("DELETE FROM msg");
		});

		Calendar cal = Calendar.getInstance();

		operate(() -> {
			// message with current timestamp
			DBUtils.update(
					"INSERT INTO msg (to_user_id, from_user_id, msg, oldmsg, timestamp, instanttime) VALUES (?, ?, ?, ?, ?, ?)",
					1, 1, NOW_MESSAGE, 0, cal.getTimeInMillis() / 1000L, 0);

			// a week before now
			cal.add(Calendar.DAY_OF_YEAR, -7);
			DBUtils.update(
					"INSERT INTO msg (to_user_id, from_user_id, msg, oldmsg, timestamp, instanttime) VALUES (?, ?, ?, ?, ?, ?)",
					1, 1, LAST_WEEK_MESSAGE, 0, cal.getTimeInMillis() / 1000L, 0);

			// a month earlier
			cal.add(Calendar.MONTH, -1);
			DBUtils.update(
					"INSERT INTO msg (to_user_id, from_user_id, msg, oldmsg, timestamp, instanttime) VALUES (?, ?, ?, ?, ?, ?)",
					1, 1, LAST_MONTH_MESSAGE, 0, cal.getTimeInMillis() / 1000L, 0);

			// a year earlier
			cal.add(Calendar.YEAR, -1);
			DBUtils.update(
					"INSERT INTO msg (to_user_id, from_user_id, msg, oldmsg, timestamp, instanttime) VALUES (?, ?, ?, ?, ?, ?)",
					1, 1, LAST_YEAR_MESSAGE, 0, cal.getTimeInMillis() / 1000L, 0);
		});

		List<String> messages = supply(() -> DBUtils.select("SELECT msg FROM msg", DBUtils.allString("msg")));
		assertThat(messages).as("Messages before purging").containsOnly(NOW_MESSAGE, LAST_WEEK_MESSAGE,
				LAST_MONTH_MESSAGE, LAST_YEAR_MESSAGE);
	}

	@Test
	public void testPurgeOneMonth() throws NodeException {
		try (PropertyTrx pTrx = new PropertyTrx(PurgeMessagesJob.MESSAGE_AGE_PARAM, "1")) {
			List<String> out = new ArrayList<>();
			CoreInternalSchedulerTask.purgemessages.execute(out);
		}

		List<String> messages = supply(() -> DBUtils.select("SELECT msg FROM msg", DBUtils.allString("msg")));
		assertThat(messages).as("Messages after purging").containsOnly(NOW_MESSAGE, LAST_WEEK_MESSAGE);
	}

	@Test
	public void testPurgeTwoMonths() throws NodeException {
		try (PropertyTrx pTrx = new PropertyTrx(PurgeMessagesJob.MESSAGE_AGE_PARAM, "2")) {
			List<String> out = new ArrayList<>();
			CoreInternalSchedulerTask.purgemessages.execute(out);
		}

		List<String> messages = supply(() -> DBUtils.select("SELECT msg FROM msg", DBUtils.allString("msg")));
		assertThat(messages).as("Messages after purging").containsOnly(NOW_MESSAGE, LAST_WEEK_MESSAGE, LAST_MONTH_MESSAGE);
	}
}
