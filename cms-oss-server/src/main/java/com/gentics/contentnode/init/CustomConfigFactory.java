package com.gentics.contentnode.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.config.yaml.YamlConfiguration;
import org.apache.logging.log4j.core.config.yaml.YamlConfigurationFactory;

/**
 * Extension to the {@link YamlConfigurationFactory}, which will merge the yaml
 * configuration (read from nodelog.yml) with a configuration that sets the log
 * level of the "runtime" logger to "INFO"
 */
public class CustomConfigFactory extends YamlConfigurationFactory {
	@Override
	public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
		// read the yaml configuration
		YamlConfiguration yamlConf = (YamlConfiguration) super.getConfiguration(loggerContext, source);

		// build a new configuration, which sets the loglevel of "runtime" to INFO
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		LoggerComponentBuilder runtimeLoggerBuilder = builder.newLogger("runtime", Level.INFO);
		builder.add(runtimeLoggerBuilder);
		BuiltConfiguration custom = builder.build();

		// return a composite configuration which merges the yaml configuration with the built one
		List<AbstractConfiguration> configurations = new ArrayList<>();
		configurations.add(yamlConf);
		configurations.add(custom);
		return new CompositeConfiguration(configurations);
	}
}
