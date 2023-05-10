package com.gentics.contentnode.runtime;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.rest.util.MiscUtils.merge;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.Velocity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.PropertyNodeConfig;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.jmx.SessionInfo;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.publish.InstantCRPublishing;
import com.gentics.lib.log.NodeLogger;

/**
 * Static helper class to provide the runtime configuration of CN.
 * The configuration is fetched via do=24 from the backend
 */
public class NodeConfigRuntimeConfiguration {

	/**
	 * configuration properties
	 */
	protected Properties configurationProperties = null;

	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(NodeConfigRuntimeConfiguration.class);

	/**
	 * Runtime log. This logger is special, because the loglevel for it will always be set to "INFO"
	 */
	public final static NodeLogger runtimeLog = NodeLogger.getNodeLogger("runtime");

	private static NodeConfigRuntimeConfiguration singleton;

	private static Map<String, Object> overwriteConfig;

	/**
	 * Configuration instance
	 */
	private NodeConfig nodeConfig;

	/**
	 * Get the singleton instance
	 * @return instance
	 */
	public static NodeConfigRuntimeConfiguration getDefault() {
		if (singleton != null) {
			return singleton;
		}
		singleton = new NodeConfigRuntimeConfiguration();
		try {
			singleton.initConfigurationProperties();
		} catch (NodeException e) {
			throw new RuntimeException("Error while initializing configuration.", e);
		}
		return singleton;
	}

	/**
	 * Protected constructor
	 */
	protected NodeConfigRuntimeConfiguration() {
	}

	/**
	 * Load the configuration and initialize
	 * @throws NodeException
	 */
	protected final void initConfigurationProperties() throws NodeException {
		try {
			if (nodeConfig != null) {
				nodeConfig.close();
				MBeanRegistry.unregisterMBean("System", "SessionInfo");
			}

			// load data
			Map<String, Object> data = loadConfiguration();

			nodeConfig = new PropertyNodeConfig(data);
			NodePreferences nodePreferences = nodeConfig.getDefaultPreferences();

			// check features
			for (Feature feature : Feature.values()) {
				if (feature.activatedButNotAvailable()) {
					logger.error(String.format(
							"Feature %s was activated in the configuration, but is not available. Feature will not be active.",
							feature.getName()));
					nodePreferences.setFeature(feature, false);
				}
			}

			nodeConfig.init();

			// TODO move this to PopertyNodeConfig.init
			// set configuration for the instant cr publishing disabler
			Map<?, ?> instantCRPublishingSettings = nodePreferences.getPropertyMap("instant_cr_publishing");
			if (instantCRPublishingSettings != null) {
				InstantCRPublishing.set(ObjectTransformer.getInt(instantCRPublishingSettings.get("maxErrorCount"), 0),
						ObjectTransformer.getInt(instantCRPublishingSettings.get("retryAfter"), 0));
			} else {
				InstantCRPublishing.set(0, 0);
			}

			MBeanRegistry.registerMBean(new SessionInfo(), "System", "SessionInfo");
		} catch (Exception e) {
			throw new NodeException("Error while loading Gentics Content.Node configuration", e);
		}
	}

	/**
	 * Shutdown the node config
	 * @throws NodeException
	 */
	protected final void shutdownConfig() throws NodeException {
		if (nodeConfig != null) {
			nodeConfig.close();
			MBeanRegistry.unregisterMBean("System", "SessionInfo");
			nodeConfig = null;
		}
	}

	/**
	 * Get the config
	 * @return config
	 */
	public final NodeConfig getNodeConfig() {
		return nodeConfig;
	}

	/**
	 * Get the configuration properties
	 * @return configuration properties
	 */
	public Properties getConfigurationProperties() {
		final NodeConfig nodeConfig = getNodeConfig();
		if (configurationProperties == null) {
			configurationProperties = new Properties() {
				private static final long serialVersionUID = 1L;

				public synchronized Object get(Object key) {
					return nodeConfig.getDefaultPreferences().getProperty(key.toString());
				}

				public String getProperty(String key, String defaultValue) {
					String val = nodeConfig.getDefaultPreferences().getProperty(key);

					if (val == null) {
						return defaultValue;
					}
					return val;
				}

				public String getProperty(String key) {
					return this.getProperty(key, null);
				}
			};
		}
		return configurationProperties;
	}

	/**
	 * Reload the configuration
	 * @throws NodeException
	 */
	public void reloadConfiguration() throws NodeException {
		initConfigurationProperties();
		operate(() -> CNDictionary.ensureConsistency());
	}

	/**
	 * Reset the NodeConfigRuntimeConfiguration helper.
	 */
	public static void reset() {
		singleton = null;
	}

	/**
	 * Shutdown the NodeConfigRuntimeConfiguration helper.
	 * @throws NodeException
	 */
	public static void shutdown() throws NodeException {
		if (singleton != null) {
			singleton.shutdownConfig();
			singleton = null;
		}
	}

	/**
	 * Check whether the feature is activated
	 * @param feature feature to check
	 * @return true iff the feature is activated
	 */
	public static boolean isFeature(Feature feature) {
		return getDefault().getNodeConfig().getDefaultPreferences().isFeature(feature);
	}

	/**
	 * Check whether the feature is activated for the given node
	 * @param feature feature to check
	 * @param node node to check
	 * @return true iff the feature is activated for the node
	 */
	public static boolean isFeature(Feature feature, Node node) {
		return getDefault().getNodeConfig().getDefaultPreferences().isFeature(feature, node);
	}

	/**
	 * Get NodePreferences
	 * @return node preferences
	 */
	public static NodePreferences getPreferences() {
		return getDefault().getNodeConfig().getDefaultPreferences();
	}

	/**
	 * Set config map to overwrite all other config
	 * @param config config map
	 */
	public static void overwrite(Map<String, Object> config) {
		overwriteConfig = config;
	}

	/**
	 * Load the configuration from all sources and return as map
	 * @return configuration map
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> loadConfiguration() throws NodeException {
		// load data
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).setDefaultMergeable(true);
		Map<String, Object> data;
		try {
			runtimeLog.info("Reading default configuration");
			// first read default configuration
			InputStream defaultConfigStream = NodeConfigRuntimeConfiguration.class.getResourceAsStream("config.yml");
			if (defaultConfigStream != null) {
				data = mapper.readValue(defaultConfigStream, Map.class);
			} else {
				data = new HashMap<>();
			}

			// read configuration files
			String configFiles = ConfigurationValue.CONF_FILES.get();

			// read from files
			if (!StringUtils.isBlank(configFiles)) {
				String confPath = ConfigurationValue.CONF_PATH.get();
				for (String filePath : StringUtils.split(configFiles, ", ")) {
					File configFile = null;
					if (!StringUtils.isBlank(confPath) && new File(confPath, filePath).exists()) {
						configFile = new File(confPath, filePath);
					} else {
						configFile = new File(filePath);
					}
					if (configFile.isDirectory()) {
						// if the configured file is a directory, we read all .yml files from that directory
						List<File> configurationFiles = new ArrayList<>(Arrays.asList(configFile.listFiles(file -> {
							return file.isFile() && StringUtils.endsWith(file.getName(), ".yml");
						})));
						// sort lexicographically
						Collections.sort(configurationFiles);

						// merge all files
						for (File subConfigFile : configurationFiles) {
							runtimeLog.info(String.format("Reading configuration from %s", subConfigFile.getAbsolutePath()));
							data = merge(data, mapper.readValue(subConfigFile, Map.class));
						}
					} else {
						if (!configFile.exists() && !configFile.getName().endsWith(".yml") && !configFile.getName().endsWith(".yaml")) {
							runtimeLog.warn(String.format("Ignoring non-existent directory %s", configFile.getAbsolutePath()));
						} else {
							runtimeLog.info(String.format("Reading configuration from %s", configFile.getAbsolutePath()));
							data = merge(data, mapper.readValue(configFile, Map.class));
						}
					}
				}
			} else {
				logger.warn(String.format(
						"No configuration files configured. Either set environment variable %s or system property %s",
						ConfigurationValue.CONF_FILES.getEnvVariableName(),
						ConfigurationValue.CONF_FILES.getSystemPropertyName()));
			}

			if (overwriteConfig != null) {
				data = merge(data, overwriteConfig);
			}

			return data;
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * initialize velocity
	 * @throws Exception if there is a fatal error while initializing velocity
	 */
	public static void initVelocity() throws Exception {
		NodePreferences prefs = getDefault().getNodeConfig().getDefaultPreferences();

		Map<String, Object> velocityConfig = prefs.getPropertyMap("velocity");
		for (Map.Entry<String, Object> entry : velocityConfig.entrySet()) {
			// we transform every value into a string, because some values, like the modificationCheckInterval
			// are expected to be Long, but will be provided by YAML config as Integer
			Velocity.setProperty(entry.getKey(), ObjectTransformer.getString(entry.getValue(), null));
		}

		Velocity.init();
	}
}
