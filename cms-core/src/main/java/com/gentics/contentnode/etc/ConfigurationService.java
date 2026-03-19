package com.gentics.contentnode.etc;

/**
 * Interface for implementations that load and validate configurations
 */
public interface ConfigurationService {
	/**
	 * Initialize the configuration read from the given preferences.
	 * This method will be called once upon server start and each time, the configuration is reloaded
	 * @param preferences preferences
	 */
	void init(NodePreferences preferences);
}
