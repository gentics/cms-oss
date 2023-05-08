/*
 * @author haymo
 * @date 09.09.2004
 * @version $Id: DatasourceException.java,v 1.1 2006-01-23 16:40:50 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception that might be thrown by Datasources
 */
public class DatasourceException extends NodeException {

	/**
	 * Create an instance of the exception
	 */
	public DatasourceException() {
		super();
	}

	/**
	 * Create an instance of the exception with a message
	 * @param message message of the exception
	 */
	public DatasourceException(String message) {
		super(message);
	}

	/**
	 * Create an instance of the exception with a message and a cause
	 * @param message message of the exception
	 * @param cause cause of the exception
	 */
	public DatasourceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create an instance of the exception with a cause
	 * @param cause cause of the exception
	 */
	public DatasourceException(Throwable cause) {
		super(cause);
	}
}
