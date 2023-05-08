package com.gentics.contentnode.rest.client.exceptions;

/**
 * Base class for all possible exceptions that might be thrown for REST API actions
 */
public class RestException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6188980943300436849L;

	/**
	 * Create an instance
	 * @param message message
	 */
	public RestException(String message) {
		super(message);
	}

	/**
	 * Create an instance
	 * @param message message
	 * @param cause cause
	 */
	public RestException(String message, Throwable cause) {
		super(message, cause);
	}
}
