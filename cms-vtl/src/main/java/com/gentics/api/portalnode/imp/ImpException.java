/*
 * @author marius.toader
 * @date 21.02.2005
 * @version $Id: ImpException.java,v 1.1 2006-01-23 16:40:51 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.portalnode.imp;

/**
 * Exception that might be thrown by imps.
 */
public class ImpException extends Exception {

	/**
	 * Create instance of the exception
	 */
	public ImpException() {
		super();
	}

	/**
	 * Create instance of the exception with a message
	 * @param message - the message that will be displayed
	 */
	public ImpException(String message) {
		super(message);
	}
}
