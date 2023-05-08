package com.gentics.contentnode.tests.publish.multithreading;

import static com.gentics.contentnode.tests.utils.ContentNodeMockUtils.content;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.AsynchronousJob;
import com.gentics.contentnode.etc.AsynchronousWorker;
import com.gentics.contentnode.factory.MulticonnectionTransaction;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.AsynchronousWorkerLoadMonitor;
import com.gentics.contentnode.publish.PageDistributor;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;
import com.gentics.contentnode.publish.SimplePublishInfo;
import com.gentics.contentnode.publish.WorkLoadMonitor;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.lib.genericexceptions.GenericFailureException;

/**
 * Test cases for the PageDistributor
 */
public class PageDistributorTest {
	/**
	 * Number of publish workers
	 */
	private final static int NUM_WORKER = 10;

	/**
	 * Number of pages to be added to the page distributor
	 */
	private final static int NUM_PAGES = 10000;

	/**
	 * Processing time of the background job (in ms)
	 */
	private final static int JOB_PROCESSING_TIME = 5;

	/**
	 * Queue limit
	 */
	private final static int JOB_QUEUE_LIMIT = 20;

	/**
	 * Waiting time if the queue is full (in ms)
	 */
	private final static int QUEUE_FULL_WAIT = 10;

	/**
	 * Maximum wait time
	 */
	private final static int MAXIMUM_WAIT = 100000;

	/**
	 * Dummy transaction
	 */
	private MulticonnectionTransaction transaction = mock(MulticonnectionTransaction.class);

	/**
	 * Maximum number of queued jobs
	 */
	private int maxQueuedJobs = 0;

	/**
	 * Test whether the queue limit for asynchronous workers is never exceeded
	 */
	@Test
	public void testQueueLimit() throws Exception {
		SimplePublishInfo publishInfo = new SimplePublishInfo();
		// prepare the pages
		List<Page> pages = new ArrayList<>();
		for (int i = 0; i < NUM_PAGES; i++) {
			pages.add(jobPage());
		}
		AsynchronousWorker asyncWorker = new AsynchronousWorker("Test Worker", true, JOB_QUEUE_LIMIT);
		asyncWorker.start();
		WorkLoadMonitor loadMonitor = new AsynchronousWorkerLoadMonitor(Arrays.asList(asyncWorker), QUEUE_FULL_WAIT);
		TransactionManager.setCurrentTransaction(transaction);
		PageDistributor pageDistributor = new PageDistributor(1, pages.stream().map(p -> new PublishQueue.NodeObjectWithAttributes<>(p))
				.collect(Collectors.toList()), loadMonitor, publishInfo, null, null, null, null);

		Collection<DummyWorker> workers = new ArrayList<DummyWorker>();
		for (int i = 0; i < NUM_WORKER; i++) {
			DummyWorker worker = new DummyWorker(pageDistributor, asyncWorker, false, transaction);
			workers.add(worker);
			worker.start();
		}

		// wait for all workers
		for (DummyWorker worker : workers) {
			worker.join(MAXIMUM_WAIT);
			assertFalse("Worker thread did not finish within " + MAXIMUM_WAIT + " ms", worker.isAlive());
			worker.assertSuccess();
		}

		NodeException nodeException = pageDistributor.getNodeException();
		if (nodeException != null) {
			throw nodeException;
		}
		GenericFailureException genericFailureException = pageDistributor.getGenericFailureException();
		if (genericFailureException != null) {
			throw genericFailureException;
		}

		// wait for the background jobs
		asyncWorker.flush();
		assertEquals("Check Queue after flush", 0, asyncWorker.getQueuedJobs());

		asyncWorker.throwExceptionOnFailure();

		// assert that all pages have been handled
		for (Page page : pages) {
			if (page instanceof JobPage) {
				((JobPage) page).assertHandled();
			}
		}

		// queue must not exceed limit
		// Note: the queue limit may be exceeded by the number of threads, because the page distributor will check the queue limit before
		// giving a page to a thread. Meanwhile, all other threads might add pages to the queue
		int queueLimit = JOB_QUEUE_LIMIT + NUM_WORKER;
		assertTrue("The queue exceeded the limit (had " + maxQueuedJobs + " entries, allowed were " + queueLimit + ")", maxQueuedJobs <= queueLimit);
	}

	/**
	 * Test the error behavior
	 * @throws Exception
	 */
	@Test
	public void testFailure() throws Exception {
		SimplePublishInfo publishInfo = new SimplePublishInfo();
		// prepare the pages
		List<Page> pages = new ArrayList<Page>();
		for (int i = 0; i < NUM_PAGES; i++) {
			pages.add(jobPage());
		}
		AsynchronousWorker asyncWorker = new AsynchronousWorker("Test Worker", true, JOB_QUEUE_LIMIT);
		asyncWorker.start();
		WorkLoadMonitor loadMonitor = new AsynchronousWorkerLoadMonitor(Arrays.asList(asyncWorker), QUEUE_FULL_WAIT);
		TransactionManager.setCurrentTransaction(transaction);
		PageDistributor pageDistributor = new PageDistributor(1, pages.stream().map(p -> new PublishQueue.NodeObjectWithAttributes<>(p))
				.collect(Collectors.toList()), loadMonitor, publishInfo, null, null, null, null);

		Collection<DummyWorker> workers = new ArrayList<DummyWorker>();
		for (int i = 0; i < NUM_WORKER; i++) {
			// the last worker will add a failing job to the queue of the asynchronous worker
			DummyWorker worker = new DummyWorker(pageDistributor, asyncWorker, i == NUM_WORKER - 1, transaction);
			workers.add(worker);
			worker.start();
		}

		// wait for all workers
		for (DummyWorker worker : workers) {
			worker.join(MAXIMUM_WAIT);
			assertFalse("Worker thread did not finish within " + MAXIMUM_WAIT + " ms", worker.isAlive());
			// the workers should not fail
			worker.assertSuccess();
		}

		try {
			asyncWorker.throwExceptionOnFailure();
			fail("Asynchronous Worker did not fail as expected");
		} catch (NodeException expected) {
		}
	}

	/**
	 * Set the number of currently queued jobs
	 * @param queuedJobs queued jobs
	 */
	protected synchronized void setQueuedJobs(int queuedJobs) {
		if (queuedJobs > maxQueuedJobs) {
			maxQueuedJobs = queuedJobs;
		}
	}

	/**
	 * Generate mocked page
	 * @return mocked page
	 * @throws Exception
	 */
	protected Page jobPage() throws Exception {
		Content content = content(1);
		JobPage page = mock(JobPage.class);
		when(page.getStackKeywords()).thenReturn(Page.RENDER_KEYS);
		when(page.getId()).thenReturn(1);

		when(page.getContent()).thenReturn(content);
		when(content.getPages()).thenReturn(Arrays.asList(page));

		AtomicBoolean handledByWorker = new AtomicBoolean();
		AtomicBoolean handledByJob = new AtomicBoolean();

		doAnswer(invocation -> {
			if (handledByWorker.get()) {
				throw new Exception("Page is handled twice by publish worker");
			}
			handledByWorker.set(true);

			return null;
		}).when(page).setHandledByWorker();

		doAnswer(invocation -> {
			if (handledByJob.get()) {
				throw new Exception("Page is handled twice by background job");
			}
			handledByJob.set(true);

			return null;
		}).when(page).setHandledByJob();

		doAnswer(invocation -> {
			if (!handledByWorker.get()) {
				fail("Page was not handled by the publish worker");
			}
			if (!handledByJob.get()) {
				fail("Page was not handled by the background job");
			}
			return null;
		}).when(page).assertHandled();

		return page;
	}

	private class DummyWorker extends Thread {
		/**
		 * Transaction
		 */
		protected Transaction t;

		/**
		 * Page Distributor
		 */
		protected PageDistributor distributor;

		/**
		 * Asynchronous Worker
		 */
		protected AsynchronousWorker asyncWorker;

		/**
		 * True if a failure is expected for this worker
		 */
		protected boolean expectFailure;

		/**
		 * Exception that was thrown while executing this worker
		 */
		protected Exception e;

		/**
		 * Create an instance of the worker
		 * @param distributor distributor
		 * @param asyncWorker asynchronous worker
		 * @param expectFailure True if a failure is expected for this worker
		 * @param t transaction
		 */
		public DummyWorker(PageDistributor distributor, AsynchronousWorker asyncWorker, boolean expectFailure, Transaction t) {
			this.distributor = distributor;
			this.asyncWorker = asyncWorker;
			this.expectFailure = expectFailure;
			this.t = t;
		}

		@Override
		public void run() {
			TransactionManager.setCurrentTransaction(t);
			NodeObjectWithAttributes<Page> pageWithAttributes = null;
			Page page = null;
			try {
				while ((pageWithAttributes = distributor.getNextPage()) != null) {
					page = pageWithAttributes.getObject();
					if (page instanceof JobPage) {
						((JobPage) page).setHandledByWorker();
						asyncWorker.addAsynchronousJob(new DummyJob(page, expectFailure));
						setQueuedJobs(asyncWorker.getQueuedJobs());
					}
				}
			} catch (Exception e) {
				this.e = e;
			}
		}

		/**
		 * Assert success of the worker
		 * @throws Exception
		 */
		public void assertSuccess() throws Exception {
			if (e != null) {
				throw e;
			}
		}
	}

	/**
	 * Dummy Job that sets the page to be handled by the background Job
	 */
	private class DummyJob implements AsynchronousJob {
		/**
		 * Handled page
		 */
		protected Page page;

		/**
		 * True if a failure is expected for this worker
		 */
		protected boolean expectFailure;

		/**
		 * Create a job instance
		 * @param page page
		 * @param expectFailure True if a failure is expected for this worker
		 */
		public DummyJob(Page page, boolean expectFailure) {
			this.page = page;
			this.expectFailure = expectFailure;
		}

		@Override
		public int process(RenderResult renderResult) throws Exception {
			Thread.sleep(JOB_PROCESSING_TIME);
			if (expectFailure) {
				throw new Exception("This is the expected failure");
			}
			if (page instanceof JobPage) {
				((JobPage) page).setHandledByJob();
			}
			return 1;
		}

		@Override
		public String getDescription() {
			return "Test job";
		}

		@Override
		public boolean isLogged() {
			return true;
		}
	}

	/**
	 * Interface extending Page to add test methods
	 */
	public static interface JobPage extends Page {
		/**
		 * Called by the worker, when page is handled
		 * @throws Exception
		 */
		void setHandledByWorker() throws Exception;

		/**
		 * Called by the job, when page is handled
		 * @throws Exception
		 */
		void setHandledByJob() throws Exception;

		/**
		 * Assert that page was handled by worker and job
		 */
		void assertHandled();
	}
}
