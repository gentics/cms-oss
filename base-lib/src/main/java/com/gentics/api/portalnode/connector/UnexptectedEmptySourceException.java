package com.gentics.api.portalnode.connector;

import com.gentics.api.lib.exception.NodeException;

public class UnexptectedEmptySourceException extends NodeException {

	/**
	 * Create an instance of the exception
	 */
	public UnexptectedEmptySourceException() {
		super();
	}

	/**
	 * Create an instance of the exception with a message
	 * @param message message of the exception
	 */
	public UnexptectedEmptySourceException(String message) {
		super(message);
	}

	/**
	 * Create an instance of the exception with a message and a cause
	 * @param message message of the exception
	 * @param cause cause of the exception
	 */
	public UnexptectedEmptySourceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create an instance of the exception with a cause
	 * @param cause cause of the exception
	 */
	public UnexptectedEmptySourceException(Throwable cause) {
		super(cause);
	}

}
