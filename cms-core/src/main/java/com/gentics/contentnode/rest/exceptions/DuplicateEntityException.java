package com.gentics.contentnode.rest.exceptions;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception that is thrown when an object cannot be created due to a duplicate key/name
 */
public class DuplicateEntityException extends NodeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -89084994188278584L;

	public DuplicateEntityException() {
	}

	public DuplicateEntityException(String message) {
		super(message);
	}

	public DuplicateEntityException(Throwable cause) {
		super(cause);
	}

	public DuplicateEntityException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateEntityException(String message, String messageKey) {
		super(message, messageKey);
	}

	public DuplicateEntityException(String message, String messageKey, List<String> parameters) {
		super(message, messageKey, parameters);
	}

	public DuplicateEntityException(String message, String messageKey, String parameter) {
		super(message, messageKey, parameter);
	}
}
