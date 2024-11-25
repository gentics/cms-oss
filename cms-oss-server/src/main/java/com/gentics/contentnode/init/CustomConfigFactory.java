package com.gentics.contentnode.init;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;

/**
 * Custom Configuration Factory, which creates instances of {@link CustomConfiguration}
 */
public class CustomConfigFactory extends ConfigurationFactory {

	@Override
	protected String[] getSupportedTypes() {
		return new String[] { "*" };
	}

	@Override
	public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
		return new CustomConfiguration(loggerContext, source);
	}
}
