package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.tests.assertj.GCNAssertions;
import com.gentics.contentnode.tests.assertj.PageAssert;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for content staging of pages with different stati
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.MULTICHANNELLING })
@RunWith(value = Parameterized.class)
public class ContentStagingPublishedStatusTest extends AbstractContentStagingTest {

	@Parameters(name = "{index}: publish {0}, modify {1}, take offline: {2}, offline at (before publish at) {3}, publish at {3}, offline at (after publish at) {5}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (boolean publish : Arrays.asList(true, false)) {
			for (boolean modify : Arrays.asList(true, false)) {
				for (boolean takeOffline : Arrays.asList(true, false)) {
					for (boolean offlineAtBefore : Arrays.asList(true, false)) {
						for (boolean publishAt : Arrays.asList(true, false)) {
							for (boolean offlineAtAfter : Arrays.asList(true, false)) {
								// remove unnecessary combinations
								if (modify && !publish) {
									// modifying only makes sense when the page is published first
									continue;
								}

								if (takeOffline && !publish) {
									// taking offline only makes sense when the page is published first
									continue;
								}

								if (takeOffline && offlineAtBefore) {
									// no use in taking a page offline twice
									continue;
								}

								if (publishAt && publish && !takeOffline && !offlineAtBefore && !modify) {
									// republishing a unmodified page does not make sense
									continue;
								}

								if (offlineAtBefore && (!publish || takeOffline)) {
									// "take offline at" does not make sense for offline pages
									continue;
								}

								if (offlineAtAfter && offlineAtBefore) {
									// "take offline at" can only be done once
									continue;
								}

								if (offlineAtAfter && (!publish || takeOffline) && !publishAt) {
									// "taking offline at" only makes sense for pages, which are online or will be "published at"
									continue;
								}

								data.add(new Object[] {publish, modify, takeOffline, offlineAtBefore, publishAt, offlineAtAfter});
							}
						}
					}
				}
			}
		}

		return data;
	}

	@Parameter(0)
	public boolean publish;

	@Parameter(1)
	public boolean modify;

	@Parameter(2)
	public boolean takeOffline;

	@Parameter(3)
	public boolean offlineAtBefore;

	@Parameter(4)
	public boolean publishAt;

	@Parameter(5)
	public boolean offlineAtAfter;

	protected GlobalId pageGlobalId;

	@After
	public void removeData() throws NodeException {
		supply(user, () -> new ContentStagingResourceImpl().delete("MyPackage"));

		if (pageGlobalId != null) {
			operate(t -> {
				Page page = t.getObject(Page.class, pageGlobalId);
				if (page != null) {
					page.delete(true);
				}
			});
			pageGlobalId = null;
		}
	}

	@Test
	public void testKeepStatus() throws Exception {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int initialPublishAtTime = now + 60;
		int modifyTime = now + 2 * 60;
		int takeOfflineTime = now + 3 * 60;
		int takeOfflineAtBeforeTime = now + 4 * 60;
		int publishAtTime = now + 5 * 60;
		int takeOfflineAtAfterTime = now + 6 * 60;

		// create the test page
		Page page = create(Page.class, p -> {
			p.setFolderId(folder1.getId());
			p.setTemplateId(template.getId());
			p.setName("Original Page");
		}).unlock().build();

		pageGlobalId = execute(Page::getGlobalId, page);

		// do the required actions to set the initial state of the page
		if (publish) {
			page = update(page, p -> {
			}).publish().unlock().build();
		} else if (publishAt) {
			page = update(page, p -> {
			}).publish(initialPublishAtTime).unlock().build();
		}

		if (modify) {
			page = update(page, p -> {
				p.setName("Modified Page");
			}).at(modifyTime).unlock().build();
		}

		if (takeOffline) {
			page = update(page, p -> {
			}).at(takeOfflineTime).takeOffline().unlock().build();
		}

		if (offlineAtBefore) {
			page = update(page, p -> {
			}).takeOffline(takeOfflineAtBeforeTime).unlock().build();
		}

		if (publishAt) {
			page = update(page, p -> {
			}).publish(publishAtTime).unlock().build();
		}

		if (offlineAtAfter) {
			page = update(page, p -> {
			}).takeOffline(takeOfflineAtAfterTime).unlock().build();
		}

		// assert that initial state of page is correct
		doAssertions(page, "Page before content staging", publishAtTime, publish && modify ? "2.0" : "1.0",
				offlineAtBefore ? takeOfflineAtBeforeTime : takeOfflineAtAfterTime, Staging.before);

		// put page into content package
		directCreate("MyPackage", DEFAULT_PACKAGE_DESCRIPTION);
		supply(() -> new ContentStagingResourceImpl().add("MyPackage", "page", pageGlobalId.toString(), true));
		supply(() -> new ContentStagingResourceImpl().exportFromCms("MyPackage", 10000));

		// delete page
		consume(p -> p.delete(true), page);

		// import from content package
		supply(() -> new ContentStagingResourceImpl().importIntoCms("MyPackage", 10000, false));

		// reload page
		page = supply(t -> t.getObject(Page.class, pageGlobalId));

		// assert that page exists and is in correct status
		doAssertions(page, "Page after content staging", publishAtTime,
				publish && modify && !takeOffline ? "2.0" : "1.0",
				offlineAtBefore ? takeOfflineAtBeforeTime : takeOfflineAtAfterTime, Staging.after);
	}

	/**
	 * Do the assertions on the page status and content
	 * @param page page to check
	 * @param description page description
	 * @param publishAtTime expected publish at time (if set)
	 * @param publishAtVersion expected publish at version (if set)
	 * @param takeOfflineAtTime expected take offline at time (if set)
	 * @param staging before or after content staging?
	 * @throws NodeException
	 */
	protected void doAssertions(Page page, String description, int publishAtTime, String publishAtVersion,
			int takeOfflineAtTime, Staging staging) throws NodeException {
		PageAssert assertThatPage = GCNAssertions.assertThat(page).as(description);

		operate(() -> {
			assertThatPage.isNotNull();
			GCNAssertions.assertThat(page.getContent()).as(description).hasFieldOrPropertyWithValue("locked", false);

			if (publish && !takeOffline) {
				assertThatPage.isOnline();
			} else {
				assertThatPage.isOffline();
			}
			if (modify && !publishAt && (staging == Staging.before || !takeOffline)) {
				assertThatPage.isModified();
			} else {
				assertThatPage.isNotModified();
			}
			if (modify) {
				assertThatPage.hasFieldOrPropertyWithValue("name", "Modified Page");
			} else {
				assertThatPage.hasFieldOrPropertyWithValue("name", "Original Page");
			}
			if (publishAt || offlineAtBefore || offlineAtAfter) {
				assertThatPage.isPlanned();
			} else {
				assertThatPage.isNotPlanned();
			}
			if (publishAt) {
				assertThatPage.hasPublishAt(publishAtTime, publishAtVersion);
			} else {
				assertThatPage.hasNoPublishAt();
			}
			if (offlineAtBefore || offlineAtAfter) {
				assertThatPage.hasOfflineAt(takeOfflineAtTime);
			} else {
				assertThatPage.hasNoOfflineAt();
			}
		});
	}

	/**
	 * Enum for asserting before or after content staging
	 */
	protected static enum Staging {
		/**
		 * Assert before content staging
		 */
		before,

		/**
		 * Assert after content staging
		 */
		after
	}
}
