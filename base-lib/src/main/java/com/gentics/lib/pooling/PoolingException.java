/*
 * @author norbert
 * @date 06.12.2007
 * @version $Id: PoolingException.java,v 1.2 2008-01-09 09:11:19 norbert Exp $
 */
package com.gentics.lib.pooling;

/**
 * Exception that might be thrown while accessing a Pool
 */
public class PoolingException extends Exception {

	/**
	 * Create instance of the exception
	 */
	public PoolingException() {}

	/**
	 * Create instance of the exception
	 * @param message message 
	 * @param cause cause
	 */
	public PoolingException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create instance of the exception
	 * @param message message
	 */
	public PoolingException(String message) {
		super(message);
	}

	/**
	 * Create instance of the exception
	 * @param cause cause
	 */
	public PoolingException(Throwable cause) {
		super(cause);
	}
}
