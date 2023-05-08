/*
 * @author laurin
 * @date 29.03.2005
 * @version $Id: UnavailableException.java,v 1.2 2006-01-13 17:50:11 laurin Exp $
 */
package com.gentics.lib.genericexceptions;

import com.gentics.api.lib.exception.NodeException;

/**
 * @author laurin something like a file, dataset, class etc is unavailable for
 *         different reasons, but shouldn't be.
 */
public class UnavailableException extends NodeException {

	/**
	 * 
	 */
	public UnavailableException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public UnavailableException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public UnavailableException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public UnavailableException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}
}
