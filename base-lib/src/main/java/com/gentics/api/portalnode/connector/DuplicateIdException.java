package com.gentics.api.portalnode.connector;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception that is thrown when registering a datasource or handle with an already existing ID
 */
public class DuplicateIdException extends NodeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5504772828987623848L;

	/**
	 * Create an instance with the given message
	 * @param message message
	 */
	public DuplicateIdException(String message) {
		super(message);
	}
}
