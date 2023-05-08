package com.gentics.contentnode.rest.client.exceptions;

/**
 * Exception that is thrown when the session identification is missing or invalid
 */
public class AuthRequiredRestException extends RestException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4328463225048672267L;

	/**
	 * Create an instance
	 * @param message message
	 */
	public AuthRequiredRestException(String message) {
		super(message);
	}
}
