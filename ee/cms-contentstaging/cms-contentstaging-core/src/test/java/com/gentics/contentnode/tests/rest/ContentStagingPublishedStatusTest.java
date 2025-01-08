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
@GCNFeature(set = { Feature.CONTENT_STAGING })
@RunWith(value = Parameterized.class)
public class ContentStagingPublishedStatusTest extends AbstractContentStagingTest {

	@Parameters(name = "{index}: publish {0}, modify {1}, take offline: {2}, publish at {3}, offline at {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (boolean publish : Arrays.asList(true, false)) {
			for (boolean modify : Arrays.asList(true, false)) {
				for (boolean takeOffline : Arrays.asList(true, false)) {
					for (boolean publishAt : Arrays.asList(true, false)) {
						for (boolean offlineAt : Arrays.asList(true, false)) {
							// remove unnecessary combinations
							if (modify && !publish) {
								// modifying only makes sense when the page is published first
								continue;
							}

							if (takeOffline && !publish) {
								// taking offline only makes sense when the page is published first
								continue;
							}

							if (publishAt && publish && !takeOffline && !modify) {
								// republishing a unmodified page does not make sense
								continue;
							}

							data.add(new Object[] {publish, modify, takeOffline, publishAt, offlineAt});
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
	public boolean publishAt;

	@Parameter(4)
	public boolean offlineAt;

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
		int publishAtTime = now + 4 * 60;
		int takeOfflineAtTime = now + 5 * 60;

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

		if (publishAt) {
			page = update(page, p -> {
			}).publish(publishAtTime).unlock().build();
		}

		if (offlineAt) {
			page = update(page, p -> {
			}).takeOffline(takeOfflineAtTime).unlock().build();
		}

		// assert that initial state of page is correct
		doAssertions(page, "Page before content staging", publishAtTime, takeOfflineAtTime);

		// put page into content package
		directCreate("MyPackage", DEFAULT_PACKAGE_DESCRIPTION);
		supply(() -> new ContentStagingResourceImpl().add("MyPackage", "page", pageGlobalId.toString(), true));
		supply(() -> new ContentStagingResourceImpl().exportFromCms("MyPackage", 10000));

		// delete page
		consume(p -> p.delete(true), page);

		// import from content package
		supply(() -> new ContentStagingResourceImpl().importIntoCms("MyPackage", 10000));

		// reload page
		page = supply(t -> t.getObject(Page.class, pageGlobalId));

		// assert that page exists and is in correct status
		doAssertions(page, "Page after content staging", publishAtTime, takeOfflineAtTime);
	}

	/**
	 * Do the assertions on the page status and content
	 * @param page page to check
	 * @param description page description
	 * @param publishAtTime expected publish at time (if set)
	 * @param takeOfflineAtTime expected take offline at time (if set)
	 * @throws NodeException
	 */
	protected void doAssertions(Page page, String description, int publishAtTime, int takeOfflineAtTime) throws NodeException {
		PageAssert assertThatPage = GCNAssertions.assertThat(page).as(description);

		operate(() -> {
			assertThatPage.isNotNull();
			GCNAssertions.assertThat(page.getContent()).as(description).hasFieldOrPropertyWithValue("locked", false);

			if (publish && !takeOffline) {
				assertThatPage.isOnline();
			} else {
				assertThatPage.isOffline();
			}
			if (modify && !publishAt) {
				assertThatPage.isModified();
			} else {
				assertThatPage.isNotModified();
			}
			if (modify) {
				assertThatPage.hasFieldOrPropertyWithValue("name", "Modified Page");
			} else {
				assertThatPage.hasFieldOrPropertyWithValue("name", "Original Page");
			}
			if (publishAt || offlineAt) {
				assertThatPage.isPlanned();
			} else {
				assertThatPage.isNotPlanned();
			}
			if (publishAt) {
				assertThatPage.hasPublishAt(publishAtTime, publish && modify ? "2.0" :"1.0");
			} else {
				assertThatPage.hasNoPublishAt();
			}
			if (offlineAt) {
				assertThatPage.hasOfflineAt(takeOfflineAtTime);
			} else {
				assertThatPage.hasNoOfflineAt();
			}
		});
	}
}
