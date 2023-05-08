/*
 * @author Erwin Mascher
 * @date 23.08.2004
 * @version $Id: ParserException.java,v 1.1 2006-01-23 16:40:50 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.exception;

/**
 * Exception that is thrown when a parsing process fails
 */
public class ParserException extends NodeException {
	protected String where;

	protected int pos;

	/**
	 * Create an instance of the exception with a message and a position
	 * @param message message of the exception
	 * @param where source
	 * @param pos position
	 */
	public ParserException(String message, String where, int pos) {
		super(message);
		this.where = where;
		this.pos = pos;
	}

	/**
	 * Create an instance of the exception with a message
	 * @param message message of the exception
	 */
	public ParserException(String message) {
		super(message);
	}

	/**
	 * Create an instance of the exception with a message and a cause
	 * @param message message of the exception
	 * @param cause cause of the exception
	 */
	public ParserException(String message, Throwable cause) {
		super(message, cause);
	}
}
