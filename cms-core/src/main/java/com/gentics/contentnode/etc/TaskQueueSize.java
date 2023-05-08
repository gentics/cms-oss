package com.gentics.contentnode.etc;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Generic implementation that tracks the size of a task queue.
 */
public class TaskQueueSize {
	/**
	 * Number of scheduled tasks
	 */
	protected AtomicInteger scheduledTasks = new AtomicInteger(0);

	/**
	 * Number of finished tasks
	 */
	protected AtomicInteger finishedTasks = new AtomicInteger(0);

	/**
	 * Lock
	 */
	final protected Lock lock = new ReentrantLock();

	/**
	 * Empty condition
	 */
	final protected Condition empty = lock.newCondition();

	/**
	 * Increase the number of scheduled tasks
	 */
	public void schedule() {
		lock.lock();
		try {
			scheduledTasks.incrementAndGet();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Increase the number of finished tasks
	 */
	public void finish() {
		lock.lock();
		try {
			finishedTasks.incrementAndGet();

			if (getRemainingTasks() == 0) {
				empty.signal();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Wait, until all scheduled tasks are finished
	 * @throws InterruptedException
	 */
	public void awaitEmpty() throws InterruptedException {
		lock.lock();
		try {
			if (getRemainingTasks() == 0) {
				return;
			}
			empty.await();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Get number of total scheduled tasks
	 * @return number of total scheduled tasks
	 */
	public int getTotalTasks() {
		return scheduledTasks.get();
	}

	/**
	 * Get number of remaining tasks
	 * @return number of remaining tasks
	 */
	public int getRemainingTasks() {
		return scheduledTasks.get() - finishedTasks.get();
	}

	/**
	 * Get number of finished tasks
	 * @return number of finished tasks
	 */
	public int getFinishedTasks() {
		return finishedTasks.get();
	}

	/**
	 * Check whether the task queue is still "busy" (remaining tasks > 0)
	 * @return true for busy
	 */
	public boolean isBusy() {
		return getRemainingTasks() > 0;
	}
}
