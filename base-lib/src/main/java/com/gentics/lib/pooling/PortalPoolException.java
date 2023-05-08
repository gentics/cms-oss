/*
 * @author norbert
 * @date 26.04.2006
 * @version $Id: PortalPoolException.java,v 1.1 2006-04-27 10:12:45 norbert Exp $
 */
package com.gentics.lib.pooling;

/**
 * Exception that might be thrown during operations on portal pools.
 */
public class PortalPoolException extends Exception {

	/**
	 * Create an instance of the exception
	 */
	public PortalPoolException() {
		super();
	}

	/**
	 * Create an instance of the exception
	 * @param message exception message
	 */
	public PortalPoolException(String message) {
		super(message);
	}

	/**
	 * Create an instance of the exception
	 * @param cause exception cause
	 */
	public PortalPoolException(Throwable cause) {
		super(cause);
	}

	/**
	 * Create an instance of the exception
	 * @param message exception message
	 * @param cause exception cause
	 */
	public PortalPoolException(String message, Throwable cause) {
		super(message, cause);
	}
}
