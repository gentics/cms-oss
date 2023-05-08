/*
 * @author laurin
 * @date 29.03.2005
 * @version $Id: InvalidSyntaxException.java,v 1.3 2006-01-13 17:50:11 laurin Exp $
 */
package com.gentics.lib.genericexceptions;

import com.gentics.api.lib.exception.NodeException;

/**
 * @author laurin common errors during syntax parsing (xml, csv, properties,
 *         etc)
 */
public class InvalidSyntaxException extends NodeException {

	/**
	 * @param cause
	 */
	public InvalidSyntaxException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	public InvalidSyntaxException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public InvalidSyntaxException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public InvalidSyntaxException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}
}
