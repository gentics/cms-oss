package com.gentics.contentnode.rest.util;

import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.NodePreferences;

public class FUMSocketOptions {
	/**
	 * Parameter Key, that is used to fetch the FILEUPLOADMANIPULATOR SOCKET options.
	 */
	private static final String FILEUPLOADMANIPULATOR_CONFIGURATION_SOCKETOPTIONS_PARAMETER = "fileupload_manipulator_socketoptions";

	/**
	 * Default socket timeout.
	 */
	private final static int defaultSocketTimeout = 15000;
    
	/**
	 * Default connection timeout.
	 */
	private final static long defaultConnectionTimeout = 5000;

	/**
	 * Configured number of retries for establishing a connection
	 */
	private final static int defaultConnectionRetry = 3;

	/**
	 * Socket timeout
	 */
	private int socketTimeout = defaultSocketTimeout;

	/**
	 * Connection timeout
	 */
	private long connectionTimeout = defaultConnectionTimeout;

	/**
	 * Connection retries
	 */
	private int connectionRetry = defaultConnectionRetry;

	public FUMSocketOptions(NodePreferences prefs) {
		Map<?, ?> fileUploadManipulatorSocketOptions = prefs.getPropertyMap(FILEUPLOADMANIPULATOR_CONFIGURATION_SOCKETOPTIONS_PARAMETER);

		if (fileUploadManipulatorSocketOptions != null) {
			socketTimeout = ObjectTransformer.getInt(fileUploadManipulatorSocketOptions.get("socketTimeout"), socketTimeout);
			connectionTimeout = ObjectTransformer.getLong(fileUploadManipulatorSocketOptions.get("connectionTimeout"), connectionTimeout);
			connectionRetry = ObjectTransformer.getInt(fileUploadManipulatorSocketOptions.get("connectionRetry"), connectionRetry);
		}
	}

	/**
	 * Get socket timeout
	 * @return socket timeout
	 */
	public int getSocketTimeout() {
		return socketTimeout;
	}

	/**
	 * Connection timeout
	 * @return connection timeout
	 */
	public long getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * Connection retry count
	 * @return connection retry count
	 */
	public int getConnectionRetry() {
		return connectionRetry;
	}
}
