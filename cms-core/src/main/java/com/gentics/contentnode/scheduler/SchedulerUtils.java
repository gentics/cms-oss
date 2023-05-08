/*
 * @author floriangutmann
 * @date Dec 4, 2009
 * @version $Id: SchedulerUtils.java,v 1.4 2010-02-01 08:43:47 norbert Exp $
 */
package com.gentics.contentnode.scheduler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.lib.log.NodeLogger;

/**
 * Class with some static helper methods for working with schedulers.
 * 
 * @author floriangutmann
 */
public class SchedulerUtils {

	public static NodeLogger logger = NodeLogger.getNodeLogger(SchedulerUtils.class);
    
	/**
	 * Shuts down a Scheduler. This method needs an initialized Transaction set as current in the TransactionManager.
	 * Calls the interrupt method on all running, interruptible jobs before shutting down. 
	 * This method blocks until all jobs are finished.
	 * @param scheduler The Scheduler to shutdown
	 */
	public static void forceShutdown(Scheduler scheduler) throws NodeException {
		// do nothing, when scheduler is null
		if (scheduler == null) {
			return;
		}
		try {
			scheduler.standby();
			List jobs = scheduler.getCurrentlyExecutingJobs();

			for (Iterator it = jobs.iterator(); it.hasNext();) {
				JobExecutionContext context = (JobExecutionContext) it.next();
				Job job = context.getJobInstance();

				if (job != null) {
					if (job instanceof InterruptableJob) {
						InterruptableJob interruptableJob = (InterruptableJob) job;

						// interrupt the job
						interruptableJob.interrupt();
						// reschedule the job

						// make a new trigger
						int newJobId = SchedulerUtils.getJobId();
						Trigger trigger = TriggerUtils.makeImmediateTrigger("ResceduledTrigger" + newJobId, 0, 1);

						trigger.setVolatility(false);
						trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

						try {
							// reschedule the job
							JobDetail detail = context.getJobDetail();

							detail.setName("ResceduledJob" + newJobId);
							scheduler.scheduleJob(context.getJobDetail(), trigger);
						} catch (SchedulerException e1) {
							NodeLogger.getNodeLogger(SchedulerUtils.class).error("Error while rescheduling interrupted job", e1);
						}
					}
				}
			}
			scheduler.shutdown(true);
		} catch (SchedulerException e) {
			throw new NodeException("Scheduler Exception occured when schuting down Scheduler", e);
		}
	}
    
	/**
	 * Gets a new unique id for a job.
	 * This method assumes that TranscationManager.getCurrentTransaction() returns a ready to use transaction.
	 * @return A new, unique id that can be used for a job
	 * @throws NodeException
	 */
	public static int getJobId() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement stmt = null;
		ResultSet rs = null;
        
		try {
			stmt = t.prepareInsertStatement("INSERT INTO backgroundjob (id, nada) VALUES (default, default)");
			stmt.execute();
			rs = stmt.getGeneratedKeys();
			rs.next();
			return rs.getInt(1);
		} catch (SQLException e) {
			throw new NodeException("Could not get a new job id", e);
		} finally {
			t.closeStatement(stmt);
			t.closeResultSet(rs);
		}
	}
}
