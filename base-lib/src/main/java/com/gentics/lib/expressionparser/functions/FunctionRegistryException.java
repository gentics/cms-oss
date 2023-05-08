/*
 * @author norbert
 * @date 03.07.2006
 * @version $Id: FunctionRegistryException.java,v 1.1 2006-07-19 09:00:04 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

/**
 * Exception that might be thrown during the registration of a function.
 */
public class FunctionRegistryException extends Exception {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = 151421954559668471L;

	/**
	 * Create an instance
	 */
	public FunctionRegistryException() {
		super();
	}

	/**
	 * Create an instance with a message
	 * @param message exception message
	 */
	public FunctionRegistryException(String message) {
		super(message);
	}

	/**
	 * Create an instance with a cause
	 * @param cause exception cause
	 */
	public FunctionRegistryException(Throwable cause) {
		super(cause);
	}

	/**
	 * Create an instance with a message and a cause
	 * @param message exception message
	 * @param cause exception cause
	 */
	public FunctionRegistryException(String message, Throwable cause) {
		super(message, cause);
	}
}
