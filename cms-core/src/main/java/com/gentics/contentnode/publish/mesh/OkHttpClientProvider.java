package com.gentics.contentnode.publish.mesh;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.rest.client.ProtocolVersion;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Protocol;

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
		return get(callTimeout, connectTimeout, writeTimeout, readTimeout, ProtocolVersion.DEFAULT);
	}

	/**
	 * Get the client instance to be used.
	 * This is either the same instance provided the last time (when called with identical timeouts) or a new instance, which may then be provided again.
	 * @param callTimeout call timeout in seconds
	 * @param connectTimeout connect timeout in seconds
	 * @param writeTimeout write timeout in seconds
	 * @param readTimeout read timeout in seconds
	 * @param protocolVersion HTTP protocol version
	 * @return client instance
	 */
	public static synchronized OkHttpClient get(long callTimeout, long connectTimeout, long writeTimeout, long readTimeout, ProtocolVersion protocolVersion) {
		String key = configKey(callTimeout, connectTimeout, writeTimeout, readTimeout, protocolVersion);

		if (lastClient == null || !StringUtils.equals(key, lastConfigKey)) {
			Builder builder = new OkHttpClient.Builder()
					.callTimeout(Duration.ofSeconds(callTimeout))
					.connectTimeout(Duration.ofSeconds(connectTimeout))
					.writeTimeout(Duration.ofSeconds(writeTimeout))
					.readTimeout(Duration.ofSeconds(readTimeout));

			if (protocolVersion != null) {
				switch (protocolVersion) {
				case DEFAULT:
					builder.protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1));
					break;
				case HTTP_1_1:
					builder.protocols(Collections.singletonList(Protocol.HTTP_1_1));
					break;
				case HTTP_2:
					builder.protocols(Collections.singletonList(Protocol.H2_PRIOR_KNOWLEDGE));
					break;
				}
			}
			lastClient = builder.build();
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
	 * @param protocolVersion client HTTP version
	 * @return config key
	 */
	protected static String configKey(long callTimeout, long connectTimeout, long writeTimeout, long readTimeout, ProtocolVersion protocolVersion) {
		return String.format("%d|%d|%d|%d|%s", callTimeout, connectTimeout, writeTimeout, readTimeout, protocolVersion.toString());
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
