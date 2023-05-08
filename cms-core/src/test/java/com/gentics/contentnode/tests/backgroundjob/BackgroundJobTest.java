/*
 * @author floriangutmann
 * @date 16.11.2009
 * @version $Id: BackgroundJobTest.java,v 1.3 2010-08-26 12:49:13 johannes2 Exp $
 */
package com.gentics.contentnode.tests.backgroundjob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.PropertyNodeConfig;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.job.BackgroundJob;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.scheduler.SchedulerUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.log.NodeLogger;

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
		SleepBackgroundJob job = new SleepBackgroundJob();

		job.addParameter(SleepBackgroundJob.PARAM_SLEEPTIME, new Integer(3));
		boolean finishedInForeground = job.execute(4);

		assertTrue("Check if job finished in foreground", finishedInForeground);
		assertEquals("Check if the jobResult has set the 'result' key to 'finishedSuccessful'.", job.getJobResult(), SleepBackgroundJob.RESULT_SUCCESSFUL);
		assertTrue("Check if no exceptions occured", job.getExceptions().isEmpty());
	}

	/**
	 * Creates a BackgroundJob that sleeps 5 seconds. The Timeout waiting for the Job is 3 Seconds. Checks if the Job finished in background and if the expected result
	 * was generated.
	 */
	@Test
	public void testBackgroundBackgroundJob() throws Exception {
		SleepBackgroundJob job = new SleepBackgroundJob();

		job.addParameter(SleepBackgroundJob.PARAM_SLEEPTIME, new Integer(5));
		boolean finishedInForeground = job.execute(3);

		assertFalse("Check if job finished in background", finishedInForeground);

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {}

		ResultSet rs = testContext.getDBSQLUtils().executeQuery("SELECT * from logcmd WHERE info='SleepBackgroundJob'");

		// Check if the log command was written by the backgroundjob
		assertTrue("Check if the log command was written", rs.next());
	}

	/**
	 * Creates a backroundjob that sleeps 10 seconds. After 5 seconds the Scheduler is interrupted (simulates tomcat shutdown). Checks if the job is marked as
	 * backgroundjob after 8 seconds of waiting. Checks if the finishedinBackground() was not called after other 4 seconds of waiting. Fire up the Scheduler again and
	 * wait 12 Seconds. Check if the finishedInBackground() was called.
	 */
	@Test
	@Ignore("This test fails because the thread can't access the database due to unknown reasons.")
	public void testShutdownBackgroundJob() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		final Scheduler sched = t.getNodeConfig().getPersistentScheduler();

		SleepBackgroundJob job = new SleepBackgroundJob();

		job.addParameter(SleepBackgroundJob.PARAM_SLEEPTIME, new Integer(10));

		// Shutdown the Scheduler after 5 seconds of waiting
		new Thread() {
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {}

				try {
					TransactionManager.setCurrentTransaction(testContext.getContext().getTransaction());
					System.out.println("Shutting down Scheduler");
					SchedulerUtils.forceShutdown(sched);
					TransactionManager.getCurrentTransaction().commit();
				} catch (NodeException e) {
					NodeLogger.getNodeLogger(BackgroundJobTest.class).error("Error shutting down Scheduler", e);
				}
			}
			;
		}.start();

		assertFalse("Check if the job did not finish in foreground", job.execute(8));

		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {}

		// Check that the log command was not written
		ResultSet rs = testContext.getDBSQLUtils().executeQuery("SELECT * from logcmd WHERE info='SleepBackgroundJob'");

		// Check if the log command was written by the backgroundjob
		assertFalse("Check if the log command was not written", rs.next());

		System.out.println("Starting up Scheduler again");
		PropertyNodeConfig config = (PropertyNodeConfig) t.getNodeConfig();

		config.initializePersistentScheduler();

		try {
			Thread.sleep(12000);
		} catch (InterruptedException e) {}

		// Check that the log command was written
		rs = testContext.getDBSQLUtils().executeQuery("SELECT * from logcmd WHERE info='SleepBackgroundJob'");
		// Check if the log command was written by the backgroundjob
		assertTrue("Check if the log command was written", rs.next());
	}

	/**
	 * Dummy Background job that just sleeps some time. The parameter {@link SleepBackgroundJob#PARAM_SLEEPTIME} has to be set.
	 * 
	 * If the job is interrupted, the sleep is canceled and on next startup the job sleeps again.
	 * 
	 * When finished, the job adds a parameter "result" that contains the constant {@link SleepBackgroundJob#RESULT_SUCCESSFUL}.
	 */
	public static class SleepBackgroundJob extends BackgroundJob {
		public static final String PARAM_SLEEPTIME = "sleeptime";
		public static final String RESULT_SUCCESSFUL = "finishedSuccessful";

		public SleepBackgroundJob() {}

		public void executeJob(JobExecutionContext jobExecutionContext) throws JobExecutionException {
			try {
				System.out.println("Starting Job: " + new Date());
				Integer sleepTime = (Integer) jobExecutionContext.getJobDetail().getJobDataMap().get(SleepBackgroundJob.PARAM_SLEEPTIME);

				if (sleepTime == null) {
					throw new Exception("Parameter Sleeptime not set!");
				}

				try {
					for (int i = 0; i < sleepTime.intValue(); i++) {
						abortWhenInterrupted();
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {}

				jobResult = RESULT_SUCCESSFUL;
				System.out.println("Finished Job: " + new Date());
			} catch (Exception e) {
				if (e instanceof JobExecutionException) {
					throw ((JobExecutionException) e);
				}
				exceptions.add(e);
			}
		}

		public void finishedInBackground(Map jobDataMap, List exceptions, Object jobResult, List<NodeMessage> messages) {
			try {
				System.out.println("Finished in background!");
				System.out.println("Result: " + jobResult);
				ActionLogger.logCmd(Events.DELETE, Node.TYPE_NODE, new Integer(1), null, "SleepBackgroundJob");
				TransactionManager.getCurrentTransaction().commit();
			} catch (NodeException e) {
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}
}
