/*
 * @author laurin
 * @date 29.03.2005
 * @version $Id: FileAccessException.java,v 1.3 2006-01-13 17:50:11 laurin Exp $
 */
package com.gentics.lib.genericexceptions;

import com.gentics.api.lib.exception.NodeException;

/**
 * @author laurin common errors during filehandling, like file doesn't exist or
 *         read and writeerrors.
 */
public class FileAccessException extends NodeException {

	/**
	 * @param cause
	 */
	public FileAccessException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	public FileAccessException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public FileAccessException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public FileAccessException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}
}
