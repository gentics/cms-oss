package com.gentics.contentnode.publish.mesh;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;

import okhttp3.OkHttpClient;

/**
 * Static helper class which provides the OkHttpClient instance.
 * As long as the timeout values keep the same, the same client instance will be returned on every call to {@link #get(long, long, long, long)}.
 */
public final class OkHttpClientProvider {
	/**
	 * Private constructor
	 */
	private OkHttpClientProvider() {
	}

	/**
	 * Get the client instance to be used.
	 * This is either the same instance provided the last time (when called with identical timeouts) or a new instance, which may then be provided again.
	 * @param callTimeout call timeout in seconds
	 * @param connectTimeout connect timeout in seconds
	 * @param writeTimeout write timeout in seconds
	 * @param readTimeout read timeout in seconds
	 * @return client instance
	 */
	public static synchronized OkHttpClient get(long callTimeout, long connectTimeout, long writeTimeout, long readTimeout) {
		String key = configKey(callTimeout, connectTimeout, writeTimeout, readTimeout);

		if (lastClient == null || !StringUtils.equals(key, lastConfigKey)) {
			lastClient = new OkHttpClient.Builder()
					.callTimeout(Duration.ofSeconds(callTimeout))
					.connectTimeout(Duration.ofSeconds(connectTimeout))
					.writeTimeout(Duration.ofSeconds(writeTimeout))
					.readTimeout(Duration.ofSeconds(readTimeout))
					.build();
			lastConfigKey = key;
		}

		return lastClient;
	}

	/**
	 * Get the config key (for checking whether the timeouts are the same as for the last call)
	 * @param callTimeout call timeout
	 * @param connectTimeout connect timeout
	 * @param writeTimeout write timeout
	 * @param readTimeout read timeout
	 * @return config key
	 */
	protected static String configKey(long callTimeout, long connectTimeout, long writeTimeout, long readTimeout) {
		return String.format("%d|%d|%d|%d", callTimeout, connectTimeout, writeTimeout, readTimeout);
	}

	/**
	 * Config key for the last provided client
	 */
	protected static String lastConfigKey;

	/**
	 * Last provided client
	 */
	protected static OkHttpClient lastClient;
}
