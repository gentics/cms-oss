/*
 * @author unknown
 * @date unknown
 * @version $Id: InsufficientPrivilegesException.java,v 1.4.10.1 2011-03-15 14:02:04 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.exception;

import java.util.List;

/**
 * Exception that is thrown when an action cannot be performed due to insufficient privileges
 */
public class InsufficientPrivilegesException extends NodeException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8946902812194234946L;

	/**
	 * Create an instance of the exception
	 */
	public InsufficientPrivilegesException() {
		super();
	}

	/**
	 * Create an instance with message, message key for localized message and parameters
	 * @param message internal message
	 * @param messageKey localized message key
	 * @param parameters parameters for the localized message
	 */
	public InsufficientPrivilegesException(String message, String messageKey,
			List<String> parameters) {
		super(message, messageKey, parameters);
	}

	/**
	 * Create an instance with message and localized message
	 * @param message internal message
	 * @param messageKey localized message key
	 * @param parameter parameter for the localized message
	 */
	public InsufficientPrivilegesException(String message, String messageKey, String parameter) {
		super(message, messageKey, parameter);
	}

	/**
	 * Create an instance with message and localized message
	 * @param message internal message
	 * @param messageKey localized message key
	 */
	public InsufficientPrivilegesException(String message, String messageKey) {
		super(message, messageKey);
	}

	/**
	 * Create an instance with message and cause
	 * @param message internal message
	 * @param cause cause
	 */
	public InsufficientPrivilegesException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create an instance with cause
	 * @param cause cause
	 */
	public InsufficientPrivilegesException(Throwable cause) {
		super(cause);
	}

	/**
	 * Create an instance of the exception with a message
	 * @param message message of the exception
	 */
	public InsufficientPrivilegesException(String message) {
		super(message);
	}
}
