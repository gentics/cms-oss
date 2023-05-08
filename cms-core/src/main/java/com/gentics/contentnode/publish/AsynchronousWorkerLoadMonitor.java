package com.gentics.contentnode.publish;

import java.util.ArrayList;
import java.util.Collection;

import com.gentics.contentnode.etc.AsynchronousWorker;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionStatistics;
import com.gentics.contentnode.factory.TransactionStatistics.Item;
import com.gentics.contentnode.render.RenderResult;

/**
 * Implementation of {@link WorkLoadMonitor} that checks whether {@link AsynchronousWorker} implementations are full
 */
public class AsynchronousWorkerLoadMonitor implements WorkLoadMonitor {
	/**
	 * Default waiting time
	 */
	protected final static int WAIT_MS = 1000;

	/**
	 * Time to wait in between checks whether any worker is full (in ms)
	 */
	protected int waitMs = WAIT_MS;

	/**
	 * Collection of workers to check
	 */
	protected Collection<AsynchronousWorker> workers;

	/**
	 * Create an instance that monitors the given list of workers
	 * @param workers list of workers to be monitored
	 */
	public AsynchronousWorkerLoadMonitor(Collection<AsynchronousWorker> workers) {
		this(workers, WAIT_MS);
	}

	/**
	 * Create an instance that monitors the given list of workers
	 * @param workers list of workers to be monitored
	 * @param waitMs waiting time when a worker is full (in ms)
	 */
	public AsynchronousWorkerLoadMonitor(Collection<AsynchronousWorker> workers, int waitMs) {
		this.workers = new ArrayList<AsynchronousWorker>(workers);
		this.waitMs = waitMs;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.WorkLoadMonitor#checkHighLoad()
	 */
	public void checkHighLoad() throws Exception {
		boolean waiting = false;
		TransactionStatistics stats = null;
		while (isWorkerFull()) {
			if (!waiting) {
				waiting = true;
				Transaction t = TransactionManager.getCurrentTransaction();
				RenderResult renderResult = t.getRenderResult();
				if (renderResult != null) {
					renderResult.info(Publisher.class, "Waiting because worker queue is full");
				}
				stats = t.getStatistics();
				if (stats != null) {
					stats.get(Item.QUEUE_FULL_WAIT).start();
				}
			}
			Thread.sleep(waitMs);
		}

		if (waiting && stats != null) {
			stats.get(Item.QUEUE_FULL_WAIT).stop();
		}
	}

	/**
	 * Check if any of the monitored workers is full
	 * @return true if any worker is full, false if not
	 */
	protected boolean isWorkerFull() {
		for (AsynchronousWorker worker : workers) {
			if (worker.isFull()) {
				return true;
			}
		}
		return false;
	}
}
