package com.gentics.contentnode.rest.resource.impl.proxy;

/**
 * POJO for configuration of a HTTP proxy to be used for the target connection
 */
public class CustomProxyProxy {
	/**
	 * Proxy host
	 */
	private String host;

	/**
	 * Proxy port
	 */
	private int port;

	/**
	 * Proxy host
	 * @return host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set proxy host
	 * @param host host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Proxy port
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set proxy port
	 * @param port port
	 */
	public void setPort(int port) {
		this.port = port;
	}
}
