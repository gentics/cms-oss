package com.gentics.lib.datasource.mccr;

import com.gentics.api.lib.datasource.DatasourceException;

/**
 * Exception that is thrown when the requested channel does not exist in the MCCR
 */
public class UnknownChannelException extends DatasourceException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4845732757535683673L;

	/**
	 * Create an instance of the exception
	 */
	public UnknownChannelException() {
		super();
	}

	/**
	 * Create an instance of the exception with a message
	 * @param message message of the exception
	 */
	public UnknownChannelException(String message) {
		super(message);
	}

	/**
	 * Create an instance of the exception with a message and a cause
	 * @param message message of the exception
	 * @param cause cause of the exception
	 */
	public UnknownChannelException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create an instance of the exception with a cause
	 * @param cause cause of the exception
	 */
	public UnknownChannelException(Throwable cause) {
		super(cause);
	}
}
