/*
 * @author norbert
 * @date 11.11.2009
 * @version $Id: CnMapPublishException.java,v 1.2 2010-01-29 15:18:46 norbert Exp $
 */
package com.gentics.api.contentnode.publish;

/**
 * Exception that may be thrown in instances of {@link CnMapPublishHandler}
 */
public class CnMapPublishException extends Exception {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = 2160606552211349497L;

	/**
	 * Creates an instance of the exception without message or cause
	 */
	public CnMapPublishException() {}

	/**
	 * Creates an instance of the exception with the given message
	 * @param message exception message
	 */
	public CnMapPublishException(String message) {
		super(message);
	}

	/**
	 * Creates an instance of the exception with the given cause
	 * @param cause exception cause
	 */
	public CnMapPublishException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates an instance of the exception with the given message and cause
	 * @param message exception message
	 * @param cause exception cause
	 */
	public CnMapPublishException(String message, Throwable cause) {
		super(message, cause);
	}
}
