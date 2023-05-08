/*
 * @author norbert
 * @date 04.04.2007
 * @version $Id: ObjectManagementException.java,v 1.2 2007-04-10 14:37:11 laurin Exp $
 */
package com.gentics.lib.datasource.object;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception that might be thrown from the {@link com.gentics.lib.datasource.object.ObjectManagementManager}.
 */
public class ObjectManagementException extends NodeException {

	/**
	 * Create instance of the exception
	 */
	public ObjectManagementException() {
		super();
	}

	/**
	 * Create instance of the exception
	 * @param message message
	 */
	public ObjectManagementException(String message) {
		super(message);
	}

	/**
	 * Create instance of the exception
	 * @param cause cause
	 */
	public ObjectManagementException(Throwable cause) {
		super(cause);
	}

	/**
	 * Create instance of the exception
	 * @param message message
	 * @param cause cause
	 */
	public ObjectManagementException(String message, Throwable cause) {
		super(message, cause);
	}
}
