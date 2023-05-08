package com.gentics.contentnode.tests.timemanagement;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.init.MigrateTimeManagement;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.DB;

/**
 * Test cases for migration of old timemanagement settings
 */
@RunWith(value = Parameterized.class)
public class TimeManagementMigrationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext().omit(MigrateTimeManagement.class);

	private static Node node;

	private static Template template;

	private static Map<Boolean, Map<Boolean, Map<Boolean, Map<TimeSet, Map<TimeSet, Map<TimeSet, Page>>>>>> pages = new HashMap<>();

	private Page testedPage;

	@Parameter(0)
	public boolean published;

	@Parameter(1)
	public boolean modified;

	@Parameter(2)
	public boolean queue;

	@Parameter(3)
	public TimeSet timeStart;

	@Parameter(4)
	public TimeSet timeEnd;

	@Parameter(5)
	public TimeSet timePub;

	/**
	 * Get test variations
	 * @return test variations
	 */
	@Parameters(name = "{index}: published {0}, modified {1}, queue {2}, time_start {3}, time_end {4}, time_pub {5}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean published : Arrays.asList(true, false)) {
			for (boolean modified : Arrays.asList(true, false)) {
				if (!published && modified) {
					// if the page is not published, it can not be modified (after being published)
					continue;
				}
				for (boolean queue : Arrays.asList(false, true)) {
					// a page cannot be modified and in queue at the same time
					if (modified && queue) {
						continue;
					}
					for (TimeSet timeStart : Arrays.asList(TimeSet.NO, TimeSet.THREE_DAYS_BEFORE, TimeSet.IN_TWO_DAYS)) {
						for (TimeSet timeEnd : Arrays.asList(TimeSet.NO, TimeSet.TWO_DAYS_BEFORE, TimeSet.IN_THREE_DAYS)) {
							// time_end before time_start makes no sense
							if (timeEnd.before(timeStart)) {
								continue;
							}
							for (TimeSet timePub : Arrays.asList(TimeSet.NO, TimeSet.THREE_DAYS_BEFORE, TimeSet.IN_TWO_DAYS)) {
								if (timePub != TimeSet.NO && timePub == timeStart) {
									continue;
								}
								data.add(new Object[] {published, modified, queue, timeStart, timeEnd, timePub});
							}
						}
					}
				}
			}
		}
		return data;
	}

	/**
	 * Setup test data and run the migration
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupDataAndRunMigration() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		// prepare pages
		for (Object[] data : data()) {
			boolean published = (Boolean) data[0];
			boolean modified = (Boolean) data[1];
			boolean queue = (Boolean) data[2];
			TimeSet timeStart = (TimeSet) data[3];
			TimeSet timeEnd = (TimeSet) data[4];
			TimeSet timePub = (TimeSet) data[5];

			Page page = Trx.supply(t -> {
				t.setTimestamp(1000);
				return createPage(node.getFolder(), template, String.format("Testpage %b|%b|%b|%s|%s|%s", published, modified, queue, timeStart, timeEnd, timePub));
			});

			int pageId = page.getId();

			if (published) {
				try (Trx trx = new Trx()) {
					trx.getTransaction().setTimestamp(2000);

					// modify first to generate more versions
					page = update(page, upd -> upd.setName(String.format("Modified %s", upd.getName())));
					page = update(page, Page::publish);

					// set old status
					DBUtils.update("UPDATE page SET status = ? WHERE id = ?", Page.STATUS_TOPUBLISH, page.getId());

					trx.success();
				}
			}

			if (timePub != TimeSet.NO) {
				try (Trx trx = new Trx()) {
					DBUtils.update("UPDATE page SET time_pub = ?, status = ? WHERE id = ?", timePub.timestamp, Page.STATUS_TOPUBLISH_AT, page.getId());
					page = trx.getTransaction().getObject(page);
					trx.success();
				}
			}

			if (modified) {
				// modify twice to generate more versions
				try (Trx trx = new Trx()) {
					trx.getTransaction().setTimestamp(3000);
					page = trx.getTransaction().getObject(page, true);
					page.setName(String.format("Modified %s", page.getName()));
					page.save();

					page = trx.getTransaction().getObject(page);
					trx.success();
				}
				try (Trx trx = new Trx()) {
					trx.getTransaction().setTimestamp(4000);
					page = trx.getTransaction().getObject(page, true);
					page.setName(String.format("%s (again)", page.getName()));
					page.save();

					// set old status
					DBUtils.update("UPDATE page SET status = ? WHERE ID = ?", Page.STATUS_MODIFIED, page.getId());

					page = trx.getTransaction().getObject(page);
					trx.success();
				}
			} else if (queue) {
				try (Trx trx = new Trx()) {
					DBUtils.update("UPDATE page SET status = ? WHERE id = ?", Page.STATUS_QUEUE, page.getId());
					trx.success();
				}
			}
			if (timeStart != TimeSet.NO) {
				int newStatus = queue ? Page.STATUS_QUEUE : (published ? (modified ? Page.STATUS_MODIFIED : Page.STATUS_TIMEMANAGEMENT) : Page.STATUS_MODIFIED);
				int newOnline = published ? 2 : 0;

				Trx.operate(() -> DBUtils.update("UPDATE page SET time_start = ?, status = ?, online = ? WHERE id = ?", timeStart.timestamp, newStatus, newOnline,
						pageId));

				if (timeEnd != TimeSet.NO) {
					Trx.operate(() -> DBUtils.update("UPDATE page SET time_end = ? WHERE id = ?", timeEnd.timestamp, pageId));
				}
			} else if (timeEnd != TimeSet.NO) {
				Trx.operate(() -> DBUtils.update("UPDATE page SET time_end = ? WHERE id = ?", timeEnd.timestamp, pageId));
			}

			pages.computeIfAbsent(published, k -> new HashMap<>()).computeIfAbsent(modified, k -> new HashMap<>()).computeIfAbsent(queue, k -> new HashMap<>())
					.computeIfAbsent(timeStart, k -> new HashMap<>()).computeIfAbsent(timeEnd, k -> new HashMap<>()).put(timePub, page);
		}

		// run the migration
		Trx.operate(() -> {
			MigrateTimeManagement migrationJob = new MigrateTimeManagement();
			migrationJob.execute();
		});

		// assert that old timemanagement columns were deleted
		Trx.operate(t -> {
			try {
				for (String fieldName : Arrays.asList("status", "time_start", "time_end", "time_mon", "time_tue", "time_wed", "time_thu", "time_fri", "time_sat",
						"time_sun")) {
					assertThat(DB.fieldExists(t.getDBHandle(), "page", fieldName)).as("Existence of " + fieldName).isFalse();
				}
			} catch (SQLException e) {
				throw new NodeException(e);
			}
		});

	}

	/**
	 * Get the currently tested page
	 * @throws NodeException
	 */
	@Before
	public void getTestedPage() throws NodeException {
		testedPage = Trx.supply(() -> pages.get(published).get(modified).get(queue).get(timeStart).get(timeEnd).get(timePub).reload());
	}

	/**
	 * Test migration
	 * @throws NodeException
	 */
	@Test
	public void testMigration() throws NodeException {
		Trx.operate(() -> {
			NodeObjectVersion publishedPageVersion = testedPage.getPublishedVersion();
			NodeObjectVersion latestMajorVersion = getLatestMajorVersion(testedPage);

			if (timeStart != TimeSet.NO && timePub == TimeSet.NO) {
				// Case 1: timeStart is set, but not timePub
				if (published && queue) {
					doAssertions(false, false, true, 0, null, true, timeStart.timestamp, publishedPageVersion, false);
				} else if (!published && queue) {
					doAssertions(false, false, false, 0, null, true, timeStart.timestamp, latestMajorVersion, true);
				} else if (published && modified) {
					doAssertions(true, false, true, timeStart.timestamp, publishedPageVersion, false, 0, null, false);
				} else if (published && !modified) {
					doAssertions(false, false, true, timeStart.timestamp, publishedPageVersion, false, 0, null, false);
				} else {
					doAssertions(false, false, false, timeStart.timestamp, null, false, 0, null, false);
				}
			} else if (timeStart == TimeSet.NO && timePub != TimeSet.NO) {
				// Case 2: timeStart not set, but timePub
				if (published && queue) {
					doAssertions(false, true, true, 0, null, true, timePub.timestamp, publishedPageVersion, false);
				} else if (!published && queue) {
					doAssertions(false, false, false, 0, null, true, timePub.timestamp, latestMajorVersion, true);
				} else if (published && modified) {
					doAssertions(true, true, true, timePub.timestamp, null, false, 0, null, false);
				} else if (published && !modified) {
					doAssertions(false, true, true, timePub.timestamp, publishedPageVersion, false, 0, null, false);
				} else {
					doAssertions(false, false, false, timePub.timestamp, latestMajorVersion, false, 0, null, true);
				}
			} else if (timeStart != TimeSet.NO && timePub != TimeSet.NO) {
				// Case 3: both timeStart and timePub set
				if (timeStart.timestamp < timePub.timestamp) {
					if (published && queue) {
						doAssertions(false, false, true, 0, null, true, timeStart.timestamp, publishedPageVersion, false);
					} else if (!published && queue) {
						doAssertions(false, false, false, 0, null, true, timeStart.timestamp, latestMajorVersion, true);
					} else if (published && modified) {
						doAssertions(true, false, true, timeStart.timestamp, publishedPageVersion, false, 0, null, false);
					} else if (published && !modified) {
						doAssertions(false, false, true, timeStart.timestamp, publishedPageVersion, false, 0, null, false);
					} else {
						doAssertions(false, false, false, timeStart.timestamp, null, false, 0, null, false);
					}
				} else {
					if (published && queue) {
						doAssertions(false, false, true, 0, null, true, timeStart.timestamp, publishedPageVersion, false);
					} else if (!published && queue) {
						doAssertions(false, false, false, 0, null, true, timeStart.timestamp, latestMajorVersion, true);
					} else if (published && modified) {
						doAssertions(true, false, true, timeStart.timestamp, null, false, 0, null, false);
					} else if (published && !modified) {
						doAssertions(false, false, true, timeStart.timestamp, publishedPageVersion, false, 0, null, false);
					} else { 
						doAssertions(false, false, false, timeStart.timestamp, null, false, 0, null, false);
					}
				}
			} else {
				// Case 4: Neither timeStart nor timePub set
				if (published && queue) {
					doAssertions(false, true, true, 0, null, true, 0, null, false);
				} else if (!published && queue) {
					doAssertions(false, false, false, 0, null, true, 0, null, false);
				} else if (published && modified) {
					doAssertions(true, true, true, 0, null, false, 0, null, false);
				} else if (published && !modified) {
					doAssertions(false, true, true, 0, null, false, 0, null, false);
				} else {
					doAssertions(false, false, false, 0, null, false, 0, null, false);
				}
			}
		});
	}

	/**
	 * Do the assertions on the tested page
	 * @param expectedModified expected modified flag
	 * @param expectedOnline expected online flag
	 * @param expectPublishedVersion true iff page is expected to have a published version
	 * @param expectedTimePub expected time_pub
	 * @param expectedTimePubVersion expected time_pub_version
	 * @param expectedQueue true iff expected to be in queue
	 * @param expectedTimePubQueue expected time_pub_queue
	 * @param expectedTimePubVersionQueue expected time_pub_version_queue
	 * @param expectUnpublishedLatestMajorVersion true iff page is expected to have a last major version, which is not the published version
	 * @throws NodeException
	 */
	protected void doAssertions(boolean expectedModified, boolean expectedOnline, boolean expectPublishedVersion, int expectedTimePub,
			NodeObjectVersion expectedTimePubVersion, boolean expectedQueue, int expectedTimePubQueue, NodeObjectVersion expectedTimePubVersionQueue, boolean expectUnpublishedLatestMajorVersion)
			throws NodeException {
		NodeObjectVersion publishedPageVersion = testedPage.getPublishedVersion();
		NodeObjectVersion latestMajorVersion = getLatestMajorVersion(testedPage);

		assertThat(testedPage.isModified()).as("Page modified").isEqualTo(expectedModified);
		assertThat(testedPage.isOnline()).as("Page online").isEqualTo(expectedOnline);
		if (expectPublishedVersion) {
			assertThat(publishedPageVersion).as("Published version").isNotNull();
		} else {
			assertThat(publishedPageVersion).as("Published version").isNull();
		}

		assertThat(testedPage.getTimePub().getIntTimestamp()).as("Publish at time").isEqualTo(expectedTimePub);
		assertThat(testedPage.getTimePubVersion()).as("Publish at version").isEqualTo(expectedTimePubVersion);

		if (expectedQueue) {
			if (expectedTimePubQueue > 0) {
				assertThat(testedPage).as("Migrated page").hasQueuedPublishAt(testedPage.getEditor(), expectedTimePubQueue,
						expectedTimePubVersionQueue.getNumber());
			} else {
				assertThat(testedPage).as("Migrated page").hasQueuedPublish(testedPage.getEditor());
			}
		} else {
			assertThat(testedPage).as("Migrated page").hasNoQueuedPublish().hasNoQueuedPublishAt();
		}

		if (expectUnpublishedLatestMajorVersion) {
			assertThat(latestMajorVersion).as("Latest major version").isNotNull();
			assertThat(latestMajorVersion.isPublished()).as("Latest major version published").isFalse();
		}
		assertThat(testedPage.getTimeOff().getIntTimestamp()).as("Take offline at time").isEqualTo(timeEnd.timestamp);
	}

	/**
	 * Get the latest major version of the page
	 * @param page page
	 * @return latest major version (may be null)
	 * @throws NodeException
	 */
	protected NodeObjectVersion getLatestMajorVersion(Page page) throws NodeException {
		NodeObjectVersion[] versions = page.getVersions();
		if (!ObjectTransformer.isEmpty(versions)) {
			for (NodeObjectVersion version : versions) {
				if (version.isMajor()) {
					return version;
				}
			}
		}
		return null;
	}

	/**
	 * Possible values for times
	 */
	public static enum TimeSet {
		/**
		 * No time set
		 */
		NO(0),

		/**
		 * Time set to three days before now
		 */
		THREE_DAYS_BEFORE(-3),

		/**
		 * Time set to two days before now
		 */
		TWO_DAYS_BEFORE(-2),

		/**
		 * Time set to two days after now
		 */
		IN_TWO_DAYS(2),

		/**
		 * Time set to three days after now
		 */
		IN_THREE_DAYS(3);

		/**
		 * Timestamp
		 */
		int timestamp;

		/**
		 * Create instance
		 * @param dayDiff diff to now in days
		 */
		TimeSet(int dayDiff) {
			if (dayDiff == 0) {
				this.timestamp = 0;
			} else {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, dayDiff);
				timestamp = (int)(cal.getTime().getTime() / 1000);
			}
		}

		/**
		 * Check whether this time is before the other time
		 * @param other compare with
		 * @return true iff this is before other
		 */
		public boolean before(TimeSet other) {
			return timestamp < other.timestamp;
		}
	}
}
