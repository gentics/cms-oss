package com.gentics.node.tests.utils;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.api.portalnode.connector.PortalConnectorFactory.JobListener;
import com.gentics.lib.datasource.BackgroundSyncChecker;

/**
 * Static helper class to handle timing issues in tests
 */
public class TimingUtils {
	/**
	 * Default timeout for waiting until a background job was executed (60 seconds)
	 */
	public static final int BACKGROUNDJOB_WAIT_TIMEOUT = 60000;

	/**
	 * Waiting poll interval in ms
	 */
	public static final int WAIT_POLL_INTERVAL = 100;

	/**
	 * Job monitor for Quartz jobs
	 */
	protected static MonitoringJobListener jobMonitor;

	/**
	 * UUID of the job monitor
	 */
	protected static UUID jobMonitorUUID;

	/**
	 * Register a new job monitor instance at the default Quartz scheduler.
	 * This method will fail, if the job monitor is already registered.
	 * @throws SchedulerException
	 */
	public static synchronized void registerJobMonitor() {
		if (jobMonitor != null) {
			fail("Job Monitor already registered");
		}
		jobMonitor = new MonitoringJobListener();
		jobMonitorUUID = PortalConnectorFactory.registerJobListener(jobMonitor);
	}

	/**
	 * Unregister the job monitor instance, that was registered before. Do nothing if not job monitor was registered.
	 * @throws SchedulerException
	 */
	public static synchronized void unregisterJobMonitor() {
		if (jobMonitor != null) {
			PortalConnectorFactory.unregisterJobListener(jobMonitorUUID);
			jobMonitor = null;
			jobMonitorUUID = null;
		}
	}

	/**
	 * Pause the scheduler. This method will wait until all currently executed jobs are done
	 * @throws Exception
	 */
	public static void pauseScheduler() throws Exception {
		PortalConnectorFactory.pauseScheduler();
	}

	/**
	 * Resume the scheduler
	 * @throws Exception
	 */
	public static void resumeScheduler() throws Exception {
		PortalConnectorFactory.resumeScheduler();
	}

	/**
	 * Wait until the system time is guaranteed to deliver another second than before this call
	 * @throws Exception
	 */
	public static void waitForNextSecond() throws Exception {
		long startSecond = System.currentTimeMillis() / 1000;
		while ((System.currentTimeMillis() / 1000) == startSecond) {
			Thread.sleep(WAIT_POLL_INTERVAL);
		}
	}

	/**
	 * Wait until the BackgroundSyncChecker job for the given datasource is fired at least once.
	 * If the job is not fired within the default timeout, an exception will be thrown.
	 * For this method to work, a job monitor must have been registered first
	 * @param ds datasource
	 * @see TimingUtils#registerJobMonitor()
	 */
	public static void waitForBackgroundSyncChecker(Datasource ds) {
		waitForBackgroundSyncChecker(ds, BACKGROUNDJOB_WAIT_TIMEOUT);
	}

	/**
	 * Wait until the BackgroundSyncChecker job for the given datasource is fired at least once.
	 * If the job is not fired within the specified timeout, an exception will be thrown
	 * For this method to work, a job monitor must have been registered first
	 * @param ds datasource
	 * @param timeout timeout in ms
	 * @see TimingUtils#registerJobMonitor()
	 */
	public static void waitForBackgroundSyncChecker(Datasource ds, int timeout) {
		waitForBackgroundJob(String.format("%s for %s", BackgroundSyncChecker.class.getName(), ds.getId()), timeout);
	}

	/**
	 * Wait until the Background job with given name is executed at least once.
	 * If the job is not executed within the specified timeout, an exception will be thrown
	 * For this method to work, a job monitor must have been registered first
	 * @param name job name
	 * @param timeout timeout in ms
	 * @see TimingUtils#registerJobMonitor()
	 */
	public static void waitForBackgroundJob(String name, int timeout) {
		if (jobMonitor == null) {
			fail("Cannot wait for job " + name + " to be executed, because the job monitor was not registered.");
		} else {
			jobMonitor.waitForJobExecution(name, timeout);
		}
	}

	/**
	 * JobListener implementation that will count job executions for the Quartz Scheduler
	 */
	protected static class MonitoringJobListener implements JobListener {
		/**
		 * Execution counter per job name
		 */
		protected Map<String, AtomicLong> executionCounter = new HashMap<String, AtomicLong>();

		/**
		 * Get the execution counter for the given job name. If no counter has been created for the name, a new one will be created here.
		 * @param name job name
		 * @return execution counter
		 */
		protected AtomicLong getExectionCounter(String name) {
			if (!executionCounter.containsKey(name)) {
				synchronized (executionCounter) {
					if (!executionCounter.containsKey(name)) {
						executionCounter.put(name, new AtomicLong());
					}
				}
			}

			return executionCounter.get(name);
		}

		/**
		 * Wait until the job specified by name was executed at least once.
		 * If the job is not executed within the given timeout (in ms), this will fail
		 * @param name job name
		 * @param timeout timeout in ms
		 */
		protected void waitForJobExecution(String name, int timeout) {
			// get the initial count
			long initialCount = getExectionCounter(name).get();
			long start = System.currentTimeMillis();

			while ((System.currentTimeMillis() - start) < timeout) {
				try {
					Thread.sleep(WAIT_POLL_INTERVAL);
				} catch (InterruptedException e) {
					fail("Interrupted while waiting for job " + name + " to be executed");
				}

				long newCount = getExectionCounter(name).get();
				if (newCount < initialCount) {
					fail("Count was reduced from initial value " + initialCount + " to " + newCount + " while waiting for job " + name + " to be executed.");
				} else if (newCount > initialCount) {
					return;
				}
			}

			fail("Job " + name + " failed to be executed within timeout of " + timeout + " ms");
		}

		@Override
		public void handle(Class<? extends Runnable> clazz, String name, Throwable e) {
			if (e == null) {
				getExectionCounter(name).addAndGet(1);
			}
		}
	}
}
