package com.gentics.contentnode.rest.exceptions;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception that is thrown when someone tries to modify a sub package
 */
public class CannotModifySubpackageException extends NodeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4682774347774931299L;

	/**
	 * Create an instance
	 * @param message message
	 */
	public CannotModifySubpackageException(String message) {
		super(message);
	}
}
