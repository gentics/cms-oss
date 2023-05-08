/*
 * @author unknown
 * @date unknown
 * @version $Id: UnknownPropertyException.java,v 1.3 2006-01-23 16:40:50 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.exception;

/**
 * Exception that is thrown when an unknown property should be manipulated
 */
public class UnknownPropertyException extends NodeException {

	/**
	 * Create an instance of the exception
	 */
	public UnknownPropertyException() {}

	/**
	 * Create an instance of the exception with a message
	 * @param message message of the exception
	 */
	public UnknownPropertyException(String message) {
		super(message);
	}

	/**
	 * Create an instance of the exception
	 * @param message message of the exception
	 * @param cause cause of the exception
	 */
	public UnknownPropertyException(String message, Throwable cause) {
		super(message, cause);
	}
}
