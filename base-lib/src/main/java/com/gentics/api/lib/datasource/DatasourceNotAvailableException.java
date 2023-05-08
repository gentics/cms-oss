/*
 * @author robert
 * @date 24.09.2004
 * @version $Id: DatasourceNotAvailableException.java,v 1.1 2006-01-23 16:40:50 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception that is thrown when the requested datasource is not available
 */
public class DatasourceNotAvailableException extends NodeException {

	/**
	 * Create an instance of the exception
	 */
	public DatasourceNotAvailableException() {
		super();
	}

	/**
	 * Create an instance of the exception with a message
	 * @param msg message of the exception
	 */
	public DatasourceNotAvailableException(String msg) {
		super(msg);
	}

	/**
	 * Create an instance of the exception with a message and a cause
	 * @param message message of the exception
	 * @param cause cause of the exception
	 */
	public DatasourceNotAvailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
