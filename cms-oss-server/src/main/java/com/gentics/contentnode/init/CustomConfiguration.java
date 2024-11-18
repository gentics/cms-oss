package com.gentics.contentnode.init;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.yaml.YamlConfiguration;

/**
 * Extension to the {@link YamlConfiguration}, which will add the "runtime" logger
 * with level "INFO" to the YamlConfiguration (read from nodelog.yml)
 */
public class CustomConfiguration extends YamlConfiguration {
	/**
	 * Create an instance
	 * @param loggerContext logger context
	 * @param configSource configuration source
	 */
	public CustomConfiguration(LoggerContext loggerContext, ConfigurationSource configSource) {
		super(loggerContext, configSource);
	}

	@Override
	protected void doConfigure() {
		super.doConfigure();

		addLogger("runtime",
				LoggerConfig.newBuilder().withLoggerName("runtime").withLevel(Level.INFO).withConfig(this).build());
	}

	@Override
	public Configuration reconfigure() {
		try {
			final ConfigurationSource source = getConfigurationSource().resetInputStream();
			if (source == null) {
				return null;
			}
			return new CustomConfiguration(getLoggerContext(), source);
		} catch (final IOException ex) {
			LOGGER.error("Cannot locate file {}", getConfigurationSource(), ex);
		}
		return null;
	}
}
