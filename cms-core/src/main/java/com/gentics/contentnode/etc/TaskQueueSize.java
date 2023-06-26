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
	 * "limit" of the taskqueue
	 */
	protected int limit = 0;

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
	 * Not Full condition
	 */
	final protected Condition notFull = lock.newCondition();

	/**
	 * Create instance without limit
	 */
	public TaskQueueSize() {
	}

	/**
	 * Create instance with the given limit
	 * @param limit limit
	 */
	public TaskQueueSize(int limit) {
		this.limit = limit;
	}

	/**
	 * Increase the number of scheduled tasks
	 */
	public void schedule() {
		lock.lock();
		try {
			scheduledTasks.incrementAndGet();
			if (isNotFull()) {
				notFull.signal();
			}
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

			if (isEmpty()) {
				empty.signal();
			}
			if (isNotFull()) {
				notFull.signal();
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
			if (isEmpty()) {
				return;
			}
			empty.await();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Wait, until all the task queue is not null
	 * @throws InterruptedException
	 */
	public void awaitNotFull() throws InterruptedException {
		lock.lock();
		try {
			if (isNotFull()) {
				return;
			}
			notFull.await();
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

	/**
	 * Check whether the task queue is empty
	 * @return true for empty
	 */
	public boolean isEmpty() {
		return getRemainingTasks() == 0;
	}

	/**
	 * Check whether the task queue is not considered full
	 * @return true for not full
	 */
	public boolean isNotFull() {
		if (limit > 0) {
			return getRemainingTasks() < limit;
		} else {
			return true;
		}
	}
}
