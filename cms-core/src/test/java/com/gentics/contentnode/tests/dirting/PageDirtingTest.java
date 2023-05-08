package com.gentics.contentnode.tests.dirting;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue;

/**
 * Page Dirting test
 */
public class PageDirtingTest extends AbstractPageDirtingTest {

	/**
	 * Test publishing a (unpublished) page
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		// publish the target page
		t.getObject(Page.class, targetPageId, true).publish();
		t.commit(false);
		testContext.waitForDirtqueueWorker();

		// get the dirted page ids
		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, node);

		for (Integer pageId : dependentPages) {
			Page page = t.getObject(Page.class, pageId);
			assertNotNull("Page with ID " + pageId + " was not found", page);
			assertTrue(page + " must have been dirted", dirtedPageIds.contains(pageId));
		}
	}

	/**
	 * Test taking the page offline
	 * @throws Exception
	 */
	@Test
	public void testTakeOffline() throws Exception {
		// timestamp for the publish process
		int publishTime = testStartTime + 1;
		// timestamp for taking the page offline
		int offlineTime = testStartTime + 2;

		Transaction t = TransactionManager.getCurrentTransaction();
		// publish the target page
		t.getObject(Page.class, targetPageId, true).publish();
		t.commit(false);
		// run the publish process
		testContext.publish(publishTime);

		// take the page offline
		testContext.startTransaction(offlineTime);
		t = TransactionManager.getCurrentTransaction();
		t.getObject(Page.class, targetPageId, true).takeOffline();
		t.commit(false);
		testContext.waitForDirtqueueWorker();

		// get the dirted page ids
		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, node);

		for (Integer pageId : dependentPages) {
			Page page = t.getObject(Page.class, pageId);
			assertNotNull("Page with ID " + pageId + " was not found", page);
			assertTrue(page + " must have been dirted", dirtedPageIds.contains(pageId));
		}
	}

	/**
	 * Test republishing the page
	 * @throws Exception
	 */
	@Test
	public void testRepublish() throws Exception {
		// timestamp for the publish process
		int publishTime = testStartTime + 1;
		// timestamp for republishing the page
		int republishTime = testStartTime + 2;

		Transaction t = TransactionManager.getCurrentTransaction();
		// publish the target page
		t.getObject(Page.class, targetPageId, true).publish();
		t.commit(false);
		// run the publish process
		testContext.publish(publishTime);

		// republish the page
		testContext.startTransaction(republishTime);
		t = TransactionManager.getCurrentTransaction();
		t.getObject(Page.class, targetPageId, true).publish();
		t.commit(false);
		testContext.waitForDirtqueueWorker();

		// get the dirted page ids
		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, node);

		for (Integer pageId : dependentPages) {
			Page page = t.getObject(Page.class, pageId);
			assertNotNull("Page with ID " + pageId + " was not found", page);
			if (pdateDependentPages.contains(pageId)) {
				assertTrue(page + " must have been dirted", dirtedPageIds.contains(pageId));
			} else {
				assertFalse(page + " must not have been dirted", dirtedPageIds.contains(pageId));
			}
		}
	}

	/**
	 * Test for publishing an offline page with "publish at"
	 * @throws Exception
	 */
	@Test
	public void testPublishAt() throws Exception {
		int firstPublishTime = testStartTime + 1;
		int afterFirstPublishTime = firstPublishTime + 1;
		int publishAtTime = afterFirstPublishTime + 1;
		int secondPublishTime = publishAtTime + 1;
		int afterSecondPublishTime = secondPublishTime + 1;

		Transaction t = TransactionManager.getCurrentTransaction();

		// publish the page at the timestamp
		t.getObject(Page.class, targetPageId, true).publish(publishAtTime, null);
		t.commit(false);

		// run the publish process
		testContext.publish(firstPublishTime);

		// check page contents
		testContext.startTransaction(afterFirstPublishTime);
		assertPublishedContents(false, 0);

		// run the publish process again
		testContext.publish(secondPublishTime);

		// check page contents
		testContext.startTransaction(afterSecondPublishTime);
		assertPublishedContents(true, secondPublishTime);
	}

	/**
	 * Test for republishing a published page with "publish at"
	 * @throws Exception
	 */
	@Test
	public void testRepublishAt() throws Exception {
		int firstPublishTime = testStartTime + 1;
		int afterFirstPublishTime = firstPublishTime + 1;
		int republishAtTime = afterFirstPublishTime + 1;
		int secondPublishTime = republishAtTime + 1;
		int afterSecondPublishTime = secondPublishTime + 1;

		Transaction t = TransactionManager.getCurrentTransaction();

		// publish the page
		t.getObject(Page.class, targetPageId, true).publish();
		t.commit(false);

		// run the publish process
		testContext.publish(firstPublishTime);

		// check the page contents
		t = testContext.startTransaction(afterFirstPublishTime);
		assertPublishedContents(true, testStartTime);

		// republish the page at a time
		t.getObject(Page.class, targetPageId, true).publish(republishAtTime, null);
		t.commit(false);

		// run the publish process again
		testContext.publish(secondPublishTime);

		// check page contents
		t = testContext.startTransaction(afterSecondPublishTime);
		assertPublishedContents(true, secondPublishTime);
	}
}
