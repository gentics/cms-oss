package com.gentics.lib.log;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * AutoCloseable, which will collect the log for the given NodeLoggers into a String.
 * Log Messages will be formatted with the default PatternLayout (message and newline)
 */
public class NodeLogCollector implements AutoCloseable {
	/**
	 * Name of the temporary appender
	 */
	protected final String appenderName = UUID.randomUUID().toString();

	/**
	 * Log Writer
	 */
	protected final StringWriter logWriter = new StringWriter();

	/**
	 * Optional log level to set (temporarily)
	 */
	protected Level logLevel;

	/**
	 * Map of old log levels for each logger (by name)
	 */
	protected Map<String, Level> oldLevels = new HashMap<>();

	/**
	 * Array of loggers for which the log is collected
	 */
	protected NodeLogger[] loggers;

	/**
	 * Temporary appender
	 */
	protected Appender appender;

	/**
	 * Create an instance for the given loggers
	 * @param loggers loggers
	 */
	public NodeLogCollector(NodeLogger... loggers) {
		this(null, loggers);
	}

	/**
	 * Create an instance for the given loggers
	 * @param logLevel optional log level
	 * @param loggers loggers
	 */
	public NodeLogCollector(Level logLevel, NodeLogger... loggers) {
		this.loggers = loggers;
		this.logLevel = logLevel;

		if (this.loggers.length > 0) {
			appender = WriterAppender.createAppender(PatternLayout.createDefaultLayout(), null, logWriter, appenderName,
					false, true);
			NodeLogger.addAppenderToConfig(appender);

			for (NodeLogger logger : this.loggers) {
				logger.addAppender(appender, this.logLevel);

				if (this.logLevel != null) {
					oldLevels.put(logger.getName(), logger.getLevel());
					logger.setLevel(this.logLevel);
				}
			}
		}
	}

	/**
	 * Get the collected log
	 * @return collected log
	 */
	public String getLog() {
		return logWriter.toString();
	}

	@Override
	public void close() {
		if (appender != null && this.loggers.length > 0) {
			NodeLogger.removeAppenderFromConfig(appenderName);
			for (NodeLogger logger : loggers) {
				if (logLevel != null) {
					logger.setLevel(oldLevels.getOrDefault(logger.getName(), logger.getLevel()));
				}
			}
		}
	}
}
