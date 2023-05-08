/*
 * @author norbert
 * @date 19.12.2006
 * @version $Id: TransactionException.java,v 1.1 2007-01-03 12:20:15 norbert Exp $
 */
package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;

/**
 * TODO comment this
 * @author norbert
 *
 */
public class TransactionException extends NodeException {

	/**
	 * 
	 */
	public TransactionException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public TransactionException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public TransactionException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public TransactionException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}
}
