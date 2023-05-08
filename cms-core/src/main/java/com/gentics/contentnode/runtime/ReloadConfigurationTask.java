package com.gentics.contentnode.runtime;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.lib.log.NodeLogger;

/**
 * Task implementation to reload the configuration
 */
public class ReloadConfigurationTask implements Callable<String>, Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2699589566977272003L;

	/**
	 * Log
	 */
	private final static NodeLogger log = NodeLogger.getNodeLogger(ReloadConfigurationTask.class);

	/**
	 * Create an instance
	 */
	public ReloadConfigurationTask() {
	}

	@Override
	public String call() throws Exception {
		try {
			log.debug("Reloading configuration ...");
			NodeConfigRuntimeConfiguration.getDefault().reloadConfiguration();
			// Reload config of Factories
			ContentNodeFactory.getInstance().reloadConfiguration();
			return "Successfully reloaded configuration.";
		} catch (NodeException e) {
			log.error("Error while trying to reload configuration.", e);
			return "Error while trying to reload configuration: " + e.getLocalizedMessage();
		}
	}
}