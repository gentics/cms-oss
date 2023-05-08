package com.gentics.contentnode.rest.client.exceptions;

/**
 * Exception that is thrown when it is not possible to send requests to a system because it is currently in maintenance mode
 */
public class MaintenanceModeRestException extends RestException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -450262228976398153L;

	/**
	 * Create an instance
	 * @param message message
	 */
	public MaintenanceModeRestException(String message) {
		super(message);
	}
}
