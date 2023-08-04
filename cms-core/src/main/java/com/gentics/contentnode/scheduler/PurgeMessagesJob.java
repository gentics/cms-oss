package com.gentics.contentnode.scheduler;

import static com.gentics.contentnode.db.DBUtils.deleteWithPK;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.log.ActionLogger.PURGEMESSAGES;
import static com.gentics.contentnode.log.ActionLogger.logCmd;

import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.logger.LogCollector;
import com.gentics.contentnode.logger.StringListAppender;
import com.gentics.contentnode.messaging.Message;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;

/**
 * Scheduler task to purge old inbox messages
 */
public class PurgeMessagesJob extends AbstractPurgeJob {
	/**
	 * The logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(PurgeMessagesJob.class);

	/**
	 * Configuration parameter for the message age (in months)
	 */
	public final static String MESSAGE_AGE_PARAM = "keep_inbox_messages";

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
						.parseInt(NodeConfigRuntimeConfiguration.getPreferences().getProperty(MESSAGE_AGE_PARAM));
				Calendar now = Calendar.getInstance();
				now.add(Calendar.MONTH, -logAgeInMonths);
				timestamp = (int) (now.getTimeInMillis() / 1000);
			} catch (NumberFormatException e) {
				throw new NodeException(String.format("Error while purging messages. Could not read configuration %s", MESSAGE_AGE_PARAM), e);
			}

			if (logger.isInfoEnabled()) {
				logger.info("Starting job " + getClass().getName());
			}

			purgeMessages(timestamp);

			operate(() -> logCmd(PURGEMESSAGES, Message.TYPE_INBOX_MESSAGE, 0, 0, "PurgeMessagesJob"));

			if (logger.isInfoEnabled()) {
				logger.info("Job " + getClass().getName() + " finished successfully");
			}
		}
	}

	/**
	 * Purge the msg table
	 * @param timestamp timestamp of oldest entry to keep
	 * @throws NodeException
	 */
	protected void purgeMessages(int timestamp) throws NodeException {
		batchedPurge(logger, "msg", timestamp, "timestamp < ?", new Object[] { timestamp });
	}
}
