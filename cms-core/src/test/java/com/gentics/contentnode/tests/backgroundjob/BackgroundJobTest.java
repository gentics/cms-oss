/*
 * @author floriangutmann
 * @date 16.11.2009
 * @version $Id: BackgroundJobTest.java,v 1.3 2010-08-26 12:49:13 johannes2 Exp $
 */
package com.gentics.contentnode.tests.backgroundjob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.job.AbstractBackgroundJob;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * @author floriangutmann, johannes2
 */
public class BackgroundJobTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Creates a BackgroundJob that sleeps 3 seconds. The Timeout waiting for the Job is 5 Seconds. Checks if the Job finished in Foreground and if the expected result
	 * was generated.
	 */
	@Test
	public void testForegroundBackgroundJob() throws Exception {
		SleepBackgroundJob job = new SleepBackgroundJob(3);

		AtomicBoolean foreground = new AtomicBoolean(true);
		job.execute(4, TimeUnit.SECONDS, () -> foreground.set(false));

		assertTrue("Check if job finished in foreground", foreground.get());
		assertThat(job.finished).as("Job finished").isTrue();
	}

	/**
	 * Creates a BackgroundJob that sleeps 5 seconds. The Timeout waiting for the Job is 3 Seconds. Checks if the Job finished in background and if the expected result
	 * was generated.
	 */
	@Test
	public void testBackgroundBackgroundJob() throws Exception {
		SleepBackgroundJob job = new SleepBackgroundJob(5);

		AtomicBoolean foreground = new AtomicBoolean(true);
		job.execute(4, TimeUnit.SECONDS, () -> foreground.set(false));
		assertFalse("Check if job finished in background", foreground.get());
		assertThat(job.finished).as("Job finished").isFalse();

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {}

		assertThat(job.finished).as("Job finished").isTrue();
	}

	/**
	 * Creates a backroundjob that sleeps 10 seconds. After 5 seconds the Scheduler is interrupted (simulates tomcat shutdown). Checks if the job is marked as
	 * backgroundjob after 8 seconds of waiting. Checks if the finishedinBackground() was not called after other 4 seconds of waiting. Fire up the Scheduler again and
	 * wait 12 Seconds. Check if the finishedInBackground() was called.
	 */
	@Test
	@Ignore("This test fails because the thread can't access the database due to unknown reasons.")
	public void testShutdownBackgroundJob() throws Exception {

		SleepBackgroundJob job = new SleepBackgroundJob(10);

		// Shutdown the Scheduler after 5 seconds of waiting
		new Thread() {
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {}

				System.out.println("Shutting down Operator");
				Operator.shutdown();
			}
			;
		}.start();

		AtomicBoolean foreground = new AtomicBoolean(true);
		job.execute(8, TimeUnit.SECONDS, () -> foreground.set(false));
		assertFalse("Check if the job did not finish in foreground", foreground.get());

		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {}

		assertThat(job.finished).as("Job finished").isFalse();
	}

	/**
	 * Dummy Background job that just sleeps some time. The parameter {@link SleepBackgroundJob#PARAM_SLEEPTIME} has to be set.
	 * 
	 * If the job is interrupted, the sleep is canceled and on next startup the job sleeps again.
	 * 
	 * When finished, the job adds a parameter "result" that contains the constant {@link SleepBackgroundJob#RESULT_SUCCESSFUL}.
	 */
	public static class SleepBackgroundJob extends AbstractBackgroundJob {

		protected int sleepTime;

		protected boolean finished = false;

		public SleepBackgroundJob(int sleepTime) {
			this.sleepTime = sleepTime;
		}

		@Override
		public String getJobDescription() {
			return getClass().getName();
		}

		@Override
		protected void processAction() throws NodeException {
			System.out.println("Starting Job: " + new Date());

			try {
				for (int i = 0; i < sleepTime; i++) {
					abortWhenInterrupted();
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {}

			finished = true;
			System.out.println("Finished Job: " + new Date());
		}
	}
}
