package com.gentics.contentnode.rest.client.exceptions;

/**
 * Exception that is thrown when a requested object was not found in the system
 */
public class NotFoundRestException extends RestException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 7414717970140167396L;

	/**
	 * Create an instance
	 * @param message message
	 */
	public NotFoundRestException(String message) {
		super(message);
	}
}
