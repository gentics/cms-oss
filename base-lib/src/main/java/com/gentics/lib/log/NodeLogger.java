package com.gentics.lib.log;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * Loglevels:
 * <ul>
 * <li>FATAL log very severe error events that will presumably lead the
 * application to abort.</li>
 * <li>ERROR log error events that might still allow the application to
 * continue running.</li>
 * <li>WARN log potentially harmful situations.</li>
 * <li>INFO log informational messages that highlight the progress of the
 * application at coarse-grained level.</li>
 * <li>DEBUG log fine-grained informational events that are most useful to
 * debug an application.</li>
 * </ul>
 * @author norbert
 */
public class NodeLogger {
	/**
	 * internal map of loggers
	 */
	private static Map<Logger, NodeLogger> loggers = new HashMap<>();

	/**
	 * a logger factory which will be used to create a logger if defined.
	 */
	private static NodeLoggerFactory loggerFactory = null;

	/**
	 * Set the logger factory
	 * @param factory factory
	 */
	public static void setNodeLoggerFactory(NodeLoggerFactory factory) {
		loggerFactory = factory;
	}

	/**
	 * Get the logger for the given class
	 * @param clazz class
	 * @return log4j logger
	 */
	public static Logger getLogger(Class<?> clazz) {
		if (loggerFactory != null) {
			return loggerFactory.getLogger(clazz);
		}
		return LogManager.getContext(NodeLogger.class.getClassLoader(), true).getLogger(clazz);
	}

	/**
	 * Get the log4j logger for the given name
	 * @param name logger name
	 * @return log4j loger
	 */
	public static Logger getLogger(String name) {
		if (loggerFactory != null) {
			return loggerFactory.getLogger(name);
		}
		return LogManager.getContext(NodeLogger.class.getClassLoader(), true).getLogger(name);
	}

	/**
	 * Get the NodeLogger for the given class
	 * @param clazz class
	 * @return NodeLogger
	 */
	public static NodeLogger getNodeLogger(Class<?> clazz) {
		if (loggerFactory != null) {
			return loggerFactory.getNodeLogger(clazz);
		}
		return getNodeLogger(getLogger(clazz));
	}

	/**
	 * Get the NodeLogger for the given name
	 * @param name name
	 * @return NodeLogger
	 */
	public static NodeLogger getNodeLogger(String name) {
		if (loggerFactory != null) {
			return loggerFactory.getNodeLogger(name);
		}
		return getNodeLogger(getLogger(name));
	}

	/**
	 * Get the root logger
	 * @return root logger
	 */
	public static NodeLogger getRootLogger() {
		return getNodeLogger(
				LogManager.getContext(NodeLogger.class.getClassLoader(), true).getLogger(LogManager.ROOT_LOGGER_NAME));
	}

	/**
	 * Add the appender to the configuration of the given node logger
	 * @param appender appender
	 * @param nodeLogger node logger
	 */
	public static void addAppenderToConfig(Appender appender, NodeLogger nodeLogger) {
		Configuration configuration = getConfiguration();
		configuration.addAppender(appender);
		if (!appender.isStarted()) {
			appender.start();
		}
	}

	/**
	 * Remove the appender with the given name
	 * @param appenderName appender name
	 * @param nodeLogger node logger
 	 */
	public static void removeAppenderFromConfig(String appenderName, NodeLogger nodeLogger) {
		Configuration configuration = getConfiguration();
		// remove the appender from all logger configs
		for (LoggerConfig loggerConfig : configuration.getLoggers().values()) {
			loggerConfig.removeAppender(appenderName);
		}
		// remove the appender from the configuration
		Map<String, Appender> appenders = configuration.getAppenders();
		Appender appender = appenders.remove(appenderName);

		// if the appender existed, stop it
		if (appender != null) {
			appender.stop();
		}
	}

	/**
	 * Internal helper method to fetch the nodelogger from the map or create a
	 * new one (if not yet existent)
	 * @param logger Logger to wrap
	 * @return NodeLogger instance
	 */
	protected static NodeLogger getNodeLogger(Logger logger) {
		NodeLogger nodeLogger = loggers.get(logger);

		if (nodeLogger == null) {
			nodeLogger = new NodeLogger(logger);
		}
		return nodeLogger;
	}

	/**
	 * Get the current configuration
	 * @param loader class loader
	 * @return configuration
	 */
	protected static Configuration getConfiguration() {
		LoggerContext context = LoggerContext.getContext(NodeLogger.class.getClassLoader(), true, null);
		return context.getConfiguration();
	}

	/**
	 * Create an instance wrapping the given logger
	 * @param logger wrapped logger
	 */
	protected NodeLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Wrapped log4j logger
	 */
	private Logger logger;

	/**
	 * Log an error
	 * @param message message
	 * @param throwable throwable
	 */
	public void error(Object message, Throwable throwable) {
		logger.error(message, throwable);
	}

	/**
	 * Log debug message
	 * @param message message
	 * @param throwable throwable
	 */
	public void debug(Object message, Throwable throwable) {
		logger.debug(message, throwable);
	}

	/**
	 * Log debug message
	 * @param message message
	 */
	public void debug(Object message) {
		logger.debug(message);
	}

	/**
	 * Log error message
	 * @param message message
	 */
	public void error(Object message) {
		logger.error(message);
	}

	/**
	 * Log fatal message
	 * @param message message
	 * @param throwable throwable
	 */
	public void fatal(Object message, Throwable throwable) {
		logger.fatal(message, throwable);
	}

	/**
	 * Log fatal message
	 * @param message message
	 */
	public void fatal(Object message) {
		logger.fatal(message);
	}

	/**
	 * Log info message
	 * @param message message
	 * @param throwable throwable
	 */
	public void info(Object message, Throwable throwable) {
		logger.info(message, throwable);
	}

	/**
	 * Log info message
	 * @param message message
	 */
	public void info(Object message) {
		logger.info(message);
	}
	
	/**
	 * forces the output of the given info message.
	 * @param message
	 */
	public void forceInfo(Object message) {
		Level oldLevel = logger.getLevel();

		Configurator.setLevel(logger.getName(), Level.INFO);
		logger.info(message);
		Configurator.setLevel(logger.getName(), oldLevel);
	}

	/**
	 * Check whether debug logging is enabled
	 * @return state
	 */
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	/**
	 * Check whether info logging is enabled
	 * @return state
	 */
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	/**
	 * Check whether warn logging is enabled
	 * @return state
	 */
	public boolean isWarnEnabled() {
		return logger.isEnabled(Level.WARN);
	}

	/**
	 * Check whether error logging is enabled
	 * @return state
	 */
	public boolean isErrorEnabled() {
		return logger.isEnabled(Level.ERROR);
	}

	/**
	 * Log warn message
	 * @param message message
	 * @param throwable throwable
	 */
	public void warn(Object message, Throwable throwable) {
		logger.warn(message, throwable);
	}

	/**
	 * Log warn message
	 * @param message message
	 */
	public void warn(Object message) {
		logger.warn(message);
	}

	/**
	 * Check whether the logger has the given level enabled
	 * @param level level
	 * @return true when the logger has the level enabled
	 */
	public boolean isEnabled(Level level) {
		return logger.isEnabled(level);
	}

	/**
	 * Log the message in the given level
	 * @param level level
	 * @param message message
	 */
	public void log(Level level, Object message) {
		logger.log(level, message);
	}

	/**
	 * Get the logger name
	 * @return logger name
	 */
	public String getName() {
		return logger.getName();
	}

	/**
	 * Set the level to the logger
	 * @param level level
	 */
	public void setLevel(Level level) {
		LoggerConfig loggerConfig = getLoggerConfig();
		loggerConfig.setLevel(level);

		LoggerContext context = LoggerContext.getContext(NodeLogger.class.getClassLoader(), true, null);
		context.updateLoggers();
	}

	/**
	 * Add the given appender
	 * @param appender appender
	 */
	public void addAppender(Appender appender) {
		addAppender(appender, null);
	}

	/**
	 * Add the given appender for the given level
	 * @param appender appender
	 * @param level level
	 */
	public void addAppender(Appender appender, Level level) {
		addAppenderToConfig(appender, this);
		getLoggerConfig().addAppender(appender, level, null);
		LoggerContext context = LoggerContext.getContext(false);
		context.updateLoggers();
	}

	/**
	 * Remove the appender with given name
	 * @param appenderName appender name
	 */
	public void removeAppenderFromLogger(String appenderName) {
		getLoggerConfig().removeAppender(appenderName);
	}

	/**
	 * Remove all appenders
	 */
	public void removeAllAppenders() {
		LoggerConfig loggerConfig = getLoggerConfig();
		for (String name : loggerConfig.getAppenders().keySet()) {
			loggerConfig.removeAppender(name);
		}
	}

	/**
	 * Get the current log level
	 * @return log level
	 */
	public Level getLevel() {
		return getLoggerConfig().getLevel();
	}

	/**
	 * Get the logger config
	 * @return logger config
	 */
	protected LoggerConfig getLoggerConfig() {
		LoggerContext context = LoggerContext.getContext(NodeLogger.class.getClassLoader(), true, null);
		Configuration config = context.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());

		if (StringUtils.equals(loggerConfig.getName(), logger.getName())) {
			return loggerConfig;
		}

		loggerConfig = new LoggerConfig(logger.getName(), loggerConfig.getLevel(), loggerConfig.isAdditive());
		config.addLogger(logger.getName(), loggerConfig);

		context.updateLoggers();
		return loggerConfig;
	}
}
