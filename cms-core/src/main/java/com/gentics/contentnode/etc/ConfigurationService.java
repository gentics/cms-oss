package com.gentics.contentnode.etc;

import java.util.List;

import com.gentics.contentnode.rest.model.response.Message;

/**
 * Interface for implementations that load and validate configurations
 */
public interface ConfigurationService {
	/**
	 * Initialize the configuration read from the given preferences.
	 * This method will be called once upon server start and each time, the configuration is reloaded
	 * @param preferences preferences
	 * @return list of messages, which should be shown to the caller
	 */
	List<Message> init(NodePreferences preferences);

	/**
	 * Check the configuration
	 * @return list of messages, which should be shown to the caller
	 */
	List<Message> check();
}
