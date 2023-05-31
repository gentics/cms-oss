package com.gentics.contentnode.runtime;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.CustomRequestLog;

/**
 * Enum containing configuration values, which can be either read from environment variables or system properties
 */
public enum ConfigurationValue {
	/**
	 * Http Port
	 */
	HTTP_PORT("HTTP_PORT", "com.gentics.contentnode.http.port", "server.port", () -> "8080"),

	/**
	 * Configuration path
	 */
	CONF_PATH("CONF_PATH", "com.gentics.contentnode.config.path", () -> "conf", path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Path to the dbfiles
	 */
	DBFILES_PATH("DBFILES_PATH", "com.gentics.contentnode.dbfiles.path", "config.dbfiles", () -> "data/dbfiles", path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Configuration files (comma separated list)
	 */
	CONF_FILES("CONF_FILES", "com.gentics.contentnode.config.files"),

	/**
	 * License key to use, if license key file does not exist
	 */
	LICENSEKEY("LICENSEKEY", "com.gentics.contentnode.license-key.path"),

	/**
	 * Path to the keys directory
	 */
	KEYS_PATH("KEYS_PATH", "com.gentics.contentnode.keys.path", () -> "keys", path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Path to the cache configuration file
	 */
	CACHE_CONFIG_PATH("CACHE_CONFIG_PATH", "com.gentics.contentnode.cache.config.path", () -> CONF_PATH.get() + "cache.ccf"),

	/**
	 * Path to the directory, where the cache files will be stored
	 */
	CACHE_PATH("CACHE_PATH", "com.gentics.contentnode.cache.path", () -> "cache", path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Path to the log files
	 */
	LOGS_PATH("LOGS_PATH", "com.gentics.contentnode.logs.path", () -> "logs", path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Access Log Format
	 */
	ACCESS_LOG("ACCESS_LOG", "com.gentics.contentnode.access_log", () -> CustomRequestLog.NCSA_FORMAT),

	/**
	 * Path for static file publishing
	 */
	PUBLISH_PATH("PUBLISH_PATH", "com.gentics.contentnode.publish.path", () -> "publish", path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Path for static genticsimagestore
	 */
	GIS_PATH("GIS_PATH", "com.gentics.contentnode.gis.path", () -> PUBLISH_PATH.get() + "gis", path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Path for devtool packages
	 */
	PACKAGES_PATH("PACKAGES_PATH", "com.gentics.contentnode.packages.path", () -> "packages", path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Path for content packages
	 */
	CONTENT_PACKAGES_PATH("CONTENT_PACKAGES_PATH", "com.gentics.contentnode.content_packages.path", () -> "content-packages", path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Path for scheduler commands
	 */
	SCHEDULER_COMNANDS_PATH("SCHEDULER_COMNANDS_PATH", "com.gentics.contentnode.scheduler_commands.path", () -> "scheduler-commands", path -> {
		File dir = new File(path);
		return StringUtils.appendIfMissing(dir.getAbsolutePath(), "/");
	}),

	/**
	 * List of folders to statically serve (comma separated list)
	 */
	STATIC_SERVE_LIST("STATIC_SERVE_LIST", "com.gentics.contentnode.static_serve_list", () -> ""),

	/**
	 * Path to UI configuration files
	 */
	UI_CONF_PATH("UI_CONF_PATH", "com.gentics.contentnode.ui.conf_path",
			"ui.conf_path",
			() -> "ui-conf",
			path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Path to store temporary files
	 */
	TMP_PATH("TMP_PATH", "com.gentics.contentnode.tmp_path", "tmppath", () -> System.getProperty("java.io.tmpdir"),
			path -> StringUtils.appendIfMissing(path, "/")),

	/**
	 * Initial password of the node user (if not already set)
	 */
	NODE_USER_PASSWORD("NODE_USER_PASSWORD", "com.gentics.contentnode.node_user.password"),

	/**
	 * Driver Class for database connections
	 */
	NODE_DB_DRIVER_CLASS("NODE_DB_DRIVER_CLASS", "com.gentics.contentnode.db.driverClass", "db.settings.driverClass", () -> "org.mariadb.jdbc.Driver"),

	/**
	 * Database hostname
	 */
	NODE_DB_HOST("NODE_DB_HOST", "com.gentics.contentnode.db.host", "db.settings.host", () -> "localhost"),

	/**
	 * Database pool
	 */
	NODE_DB_PORT("NODE_DB_PORT", "com.gentics.contentnode.db.port", "db.settings.port", () -> "3306"),

	/**
	 * Database user
	 */
	NODE_DB_USER("NODE_DB_USER", "com.gentics.contentnode.db.user", "db.settings.user"),

	/**
	 * Database user password
	 */
	NODE_DB_PASSWORD("NODE_DB_PASSWORD", "com.gentics.contentnode.db.password", "db.settings.password"),

	/**
	 * Database name
	 */
	NODE_DB_NAME("NODE_DB_NAME", "com.gentics.contentnode.db.name", "db.settings.name", () -> "node_utf8"),

	/**
	 * Additional JDBC Parameters for accessing the backend DB
	 */
	NODE_DB_PARAMETERS("NODE_DB_PARAMETERS", "com.gentics.contentnode.db.parameters", "db.settings.jdbcparameters", () -> "netTimeoutForStreamingResults=900", v -> v),

	/**
	 * Database connection URL
	 */
	NODE_DB_URL("NODE_DB_URL", "com.gentics.contentnode.db.url", "db.settings.url", () -> {
		String url = String.format("jdbc:mariadb://%s:%s/%s",
				NODE_DB_HOST.get(),
				NODE_DB_PORT.get(),
				NODE_DB_NAME.get());
		String jdbcParameters = NODE_DB_PARAMETERS.get();
		if (!StringUtils.isBlank(jdbcParameters)) {
			url = String.format("%s?%s", url, jdbcParameters);
		}
		return url;
	}, v -> v),

	/**
	 * Initialization timeout for connecting to the database
	 */
	NODE_DB_INIT_TIMEOUT("NODE_DB_INIT_TIMEOUT", "com.gentics.contentnode.db.init_timeout", () -> "60000"),

	/**
	 * Path to alohaeditor files
	 */
	ALOHAEDITOR_PATH("ALOHAEDITOR_PATH", "com.gentics.contentnode.alohaeditor.path"),

	/**
	 * Path to alohaeditor plugins
	 */
	ALOHAEDITOR_PLUGINS_PATH("ALOHAEDITOR_PLUGINS_PATH", "com.gentics.contentnode.alohaeditor.plugins.path"),

	/**
	 * Path to the gcnjsapi files
	 */
	GCNJSAPI_PATH("GCNJSAPI_PATH", "com.gentics.contentnode.gcnjsapi.path"),

	;

	/**
	 * Name of the environment variable
	 */
	private final String envVariableName;

	/**
	 * Name of the system property
	 */
	private final String systemPropertyName;

	/**
	 * Optional name of the configuration property, which shall be overwritten with the environment
	 */
	private final String configurationProperty;

	/**
	 * Supplier for the default value
	 */
	private final Supplier<String> defaultValueSupplier;

	/**
	 * Function to clean the value (if not null)
	 */
	private final Function<String, String> valueCleaner;

	/**
	 * Create instance
	 * @param envVariableName name of the env variable
	 * @param systemPropertyName name of the system property
	 * @param configurationProperty configuration property
	 * @param defaultValueSupplier supplier for the default value
	 * @param valueCleaner value cleaner
	 */
	ConfigurationValue(String envVariableName, String systemPropertyName, String configurationProperty, Supplier<String> defaultValueSupplier, Function<String, String> valueCleaner) {
		this.envVariableName = envVariableName;
		this.systemPropertyName = systemPropertyName;
		this.configurationProperty = configurationProperty;
		this.defaultValueSupplier = defaultValueSupplier;
		this.valueCleaner = valueCleaner;
	}

	/**
	 * Create instance
	 * @param envVariableName name of the env variable
	 * @param systemPropertyName name of the system property
	 */
	ConfigurationValue(String envVariableName, String systemPropertyName) {
		this(envVariableName, systemPropertyName, null, () -> null, v -> v);
	}

	/**
	 * Create instance
	 * @param envVariableName name of the env variable
	 * @param systemPropertyName name of the system property
	 * @param configurationProperty configuration property
	 */
	ConfigurationValue(String envVariableName, String systemPropertyName, String configurationProperty) {
		this(envVariableName, systemPropertyName, configurationProperty, () -> null, v -> v);
	}

	/**
	 * Create instance
	 * @param envVariableName name of the env variable
	 * @param systemPropertyName name of the system property
	 * @param configurationProperty configuration property
	 * @param defaultValueSupplier supplier for the default value
	 */
	ConfigurationValue(String envVariableName, String systemPropertyName, String configurationProperty, Supplier<String> defaultValueSupplier) {
		this(envVariableName, systemPropertyName, configurationProperty, defaultValueSupplier, v -> v);
	}

	/**
	 * Create instance
	 * @param envVariableName name of the env variable
	 * @param systemPropertyName name of the system property
	 * @param defaultValueSupplier supplier for the default value
	 * @param valueCleaner value cleaner
	 */
	ConfigurationValue(String envVariableName, String systemPropertyName, Supplier<String> defaultValueSupplier, Function<String, String> valueCleaner) {
		this(envVariableName, systemPropertyName, null, defaultValueSupplier, valueCleaner);
	}

	/**
	 * Create instance
	 * @param envVariableName name of the env variable
	 * @param systemPropertyName name of the system property
	 * @param defaultValueSupplier supplier for the default value
	 */
	ConfigurationValue(String envVariableName, String systemPropertyName, Supplier<String> defaultValueSupplier) {
		this(envVariableName, systemPropertyName, null, defaultValueSupplier, v -> v);
	}

	/**
	 * Get the value
	 * @return value
	 */
	public String get() {
		String value = null;
		if (!StringUtils.isBlank(envVariableName)) {
			value = System.getenv(envVariableName);
		}
		if (StringUtils.isBlank(value) && !StringUtils.isBlank(systemPropertyName)) {
			value = System.getProperty(systemPropertyName);
		}
		if (StringUtils.isBlank(value) && !StringUtils.isBlank(configurationProperty)) {
			value = NodeConfigRuntimeConfiguration.getPreferences().getProperty(configurationProperty);
		}
		if (StringUtils.isBlank(value) && defaultValueSupplier != null) {
			value = defaultValueSupplier.get();
		}

		if (value != null && valueCleaner != null) {
			value = valueCleaner.apply(value);
		}
		return value;
	}

	/**
	 * Get the name of the env variable
	 * @return env variable name
	 */
	public String getEnvVariableName() {
		return envVariableName;
	}

	/**
	 * Get the name of the system property
	 * @return system property name
	 */
	public String getSystemPropertyName() {
		return systemPropertyName;
	}

	/**
	 * Get the configuration property, which should be overwritten
	 * @return configuration property (may be null)
	 */
	public String getConfigurationProperty() {
		return configurationProperty;
	}
}
