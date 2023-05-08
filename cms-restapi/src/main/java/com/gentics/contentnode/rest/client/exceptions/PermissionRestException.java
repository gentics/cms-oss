package com.gentics.contentnode.rest.client.exceptions;

/**
 * Exception that is thrown when the user did not have sufficient permissions to carry out the action
 */
public class PermissionRestException extends RestException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 7490350338195828460L;

	/**
	 * Create an instance
	 * @param message message
	 */
	public PermissionRestException(String message) {
		super(message);
	}
}
