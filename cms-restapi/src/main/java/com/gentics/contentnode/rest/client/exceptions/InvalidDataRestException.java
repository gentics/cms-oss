package com.gentics.contentnode.rest.client.exceptions;

/**
 * Exception that is thrown when data for the request was invalid or insufficient
 */
public class InvalidDataRestException extends RestException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6186476217089523794L;

	/**
	 * Create an instance
	 * @param message message
	 */
	public InvalidDataRestException(String message) {
		super(message);
	}
}
