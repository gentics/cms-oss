/*
 * @author laurin
 * @date 29.03.2005
 * @version $Id: MissingConfigurationException.java,v 1.3 2006-01-13 17:50:11 laurin Exp $
 */
package com.gentics.lib.genericexceptions;

import com.gentics.api.lib.exception.NodeException;

/**
 * @author laurin a required configuration for a major or minor component was
 *         missing. usually thrown during first-time-usage when not yet
 *         configured. not thrown when configuration exists, but is invalid in
 *         any way.
 */
public class MissingConfigurationException extends NodeException {

	/**
	 * 
	 */
	public MissingConfigurationException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public MissingConfigurationException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public MissingConfigurationException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public MissingConfigurationException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}
}
