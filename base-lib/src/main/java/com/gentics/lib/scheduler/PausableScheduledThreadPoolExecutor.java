package com.gentics.lib.scheduler;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of {@link ScheduledThreadPoolExecutor} that can be paused and resumed
 */
public class PausableScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
	/**
	 * Flag to mark whether the executor is paused
	 */
	private boolean isPaused;

	/**
	 * Lock for the pause flag
	 */
	private ReentrantLock pauseLock = new ReentrantLock();

	/**
	 * Condition for the lock
	 */
	private Condition unpaused = pauseLock.newCondition();

	/**
	 * Latch for the number of active tasks
	 */
	private Latch activeTasksLatch = new Latch();

	/**
	 * Latch implementation
	 */
	private class Latch {
		private final Object synchObj = new Object();
		private int count;

		/**
		 * Wait for the count to become zero
		 * @param waitMS maximum wait time in milliseconds
		 * @return true if the count reached zero, false if not
		 * @throws InterruptedException
		 */
		public boolean awaitZero(long waitMS) throws InterruptedException {
			long startTime = System.currentTimeMillis();
			synchronized (synchObj) {
				while (count > 0) {
					if (waitMS != 0) {
						synchObj.wait(waitMS);
						long curTime = System.currentTimeMillis();
						if ((curTime - startTime) > waitMS) {
							return count <= 0;
						}
					} else {
						synchObj.wait();
					}
				}
				return count <= 0;
			}
		}

		/**
		 * Count 1 down
		 */
		public void countDown() {
			synchronized (synchObj) {
				if (--count <= 0) {
					// assert count >= 0;
					synchObj.notifyAll();
				}
			}
		}

		/**
		 * Count 1 up
		 */
		public void countUp() {
			synchronized (synchObj) {
				count++;
			}
		}
	}

	/**
	 * Create an instance
	 * @param corePoolSize core pool size
	 */
	public PausableScheduledThreadPoolExecutor(int corePoolSize) {
		super(corePoolSize);
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		pauseLock.lock();
		try {
			while (isPaused) {
				unpaused.await();
			}
		} catch (InterruptedException ie) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}

		activeTasksLatch.countUp();
		super.beforeExecute(t, r);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		try {
			super.afterExecute(r, t);
		} finally {
			activeTasksLatch.countDown();
		}
	}

	/**
	 * Pause the executor. Running tasks will continue running, but new tasks
	 * will not start until the executor is resumed.
	 */
	public void pause() {
		pauseLock.lock();
		try {
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	/**
	 * Return true when the executor is paused
	 * @return true when paused
	 */
	public boolean isPaused() {
		return isPaused;
	}

	/**
	 * Wait for all active tasks to end
	 * @param timeoutMS
	 * @return true if no more tasks are running, false if not
	 */
	public boolean await(long timeoutMS) {
		assert isPaused;
		try {
			return activeTasksLatch.awaitZero(timeoutMS);
		} catch (InterruptedException e) {
			// log e, or rethrow maybe
		}
		return false;
	}

	/**
	 * Resume the threadpool
	 */
	public void resume() {
		pauseLock.lock();
		try {
			isPaused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}
}
