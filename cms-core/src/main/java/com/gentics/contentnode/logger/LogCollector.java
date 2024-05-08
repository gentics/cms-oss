package com.gentics.contentnode.logger;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.gentics.lib.log.NodeLogger;

/**
 * AutoClosable that adds an appender to the logger
 */
public class LogCollector implements AutoCloseable {
	/**
	 * Logger
	 */
	protected NodeLogger logger;

	/**
	 * Appender
	 */
	protected Appender appender;

	/**
	 * Create an instance. add the appender to the logger
	 * @param loggerName name of the logger
	 */
	public LogCollector(String loggerName) {
		this(loggerName, new MessageAppender(PatternLayout.newBuilder().withPattern("%d %-5p - %m%n").build()));
	}

	/**
	 * Create an instance with given appender
	 * @param loggerName logger name
	 * @param appender appender
	 */
	public LogCollector(String loggerName, Appender appender) {
		logger = NodeLogger.getNodeLogger(loggerName);
		this.appender = appender;
		NodeLogger.addAppenderToConfig(appender, logger);
		logger.addAppender(appender);
	}

	@Override
	public void close() {
		// remove the appender
		NodeLogger.removeAppenderFromConfig(appender.getName(), logger);
	}
}
