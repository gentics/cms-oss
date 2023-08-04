package com.gentics.contentnode.scheduler;

import static com.gentics.contentnode.db.DBUtils.deleteWithPK;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.log.ActionLogger.LOGACTION_TYPE;
import static com.gentics.contentnode.log.ActionLogger.PURGELOGS;
import static com.gentics.contentnode.log.ActionLogger.logCmd;

import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.logger.LogCollector;
import com.gentics.contentnode.logger.StringListAppender;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;

/**
 * Scheduler task to purge old logs
 */
public class PurgeLogsJob extends AbstractPurgeJob {
	/**
	 * The logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(PurgeLogsJob.class);

	/**
	 * Configuration parameter for the logs age (in months)
	 */
	public final static String LOG_AGE_PARAM = "cn_keeplogs";

	/**
	 * Purge
	 * @param out for log output
	 * @throws NodeException
	 */
	public void purge(List<String> out) throws NodeException {
		try (LogCollector logCollector = new LogCollector(logger.getName(),
				new StringListAppender(PatternLayout.newBuilder().withPattern("%d %-5p - %m%n").build(), out))) {
			int timestamp = 0;
			try {
				int logAgeInMonths = Integer
						.parseInt(NodeConfigRuntimeConfiguration.getPreferences().getProperty(LOG_AGE_PARAM));
				Calendar now = Calendar.getInstance();
				now.add(Calendar.MONTH, -logAgeInMonths);
				timestamp = (int) (now.getTimeInMillis() / 1000);
			} catch (NumberFormatException e) {
				throw new NodeException(String.format("Error while purging logs. Could not read configuration %s", LOG_AGE_PARAM), e);
			}

			if (logger.isInfoEnabled()) {
				logger.info("Starting job " + getClass().getName());
			}

			purgeLogCmd(timestamp);
			purgeLogError(timestamp);
			purgeSchedulerExecution(timestamp);

			operate(() -> logCmd(PURGELOGS, LOGACTION_TYPE, 0, 0, "PurgeLogsJob"));

			if (logger.isInfoEnabled()) {
				logger.info("Job " + getClass().getName() + " finished successfully");
			}
		}
	}

	/**
	 * Purge the logcmd table
	 * @param timestamp timestamp of oldest entry to keep
	 * @throws NodeException
	 */
	protected void purgeLogCmd(int timestamp) throws NodeException {
		batchedPurge(logger, "logcmd", timestamp, "timestamp < ?", new Object[] { timestamp });
	}

	/**
	 * Purge the logerror table
	 * @param timestamp timestamp of oldest entry to keep
	 * @throws NodeException
	 */
	protected void purgeLogError(int timestamp) throws NodeException {
		batchedPurge(logger, "logerror", timestamp, "timestamp < ?", new Object[] { timestamp });
	}

	/**
	 * Purge the scheduler_execution table
	 * @param timestamp timestamp of oldest entry to keep
	 * @throws NodeException
	 */
	protected void purgeSchedulerExecution(int timestamp) throws NodeException {
		batchedPurge(logger, "scheduler_execution", timestamp, "starttime < ? AND endtime > ?",
				new Object[] { timestamp, 0 });
	}
}
