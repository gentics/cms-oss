package com.gentics.contentnode.runtime;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.lib.log.NodeLogger;

/**
 * Task implementation to reload the configuration
 */
public class ReloadConfigurationTask implements Callable<List<Message>>, Serializable {
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
	public List<Message> call() throws Exception {
		try {
			log.debug("Reloading configuration ...");
			List<Message> messages = NodeConfigRuntimeConfiguration.getDefault().reloadConfiguration();
			// Reload config of Factories
			ContentNodeFactory.getInstance().reloadConfiguration();
			return messages;
		} catch (NodeException e) {
			log.error("Error while trying to reload configuration.", e);
			return List.of(new Message(Type.CRITICAL,
					"Error while trying to load configuration: %s".formatted(e.getLocalizedMessage())));
		}
	}
}