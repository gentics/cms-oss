package com.gentics.contentnode.rest.client.exceptions;

/**
 * Exception that is thrown when an unexpected error has occurred (example: a database error prevented saving)
 */
public class FailureRestException extends RestException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -783875816734983752L;

	/**
	 * Create an instance
	 * @param message message
	 */
	public FailureRestException(String message) {
		super(message);
	}
}
