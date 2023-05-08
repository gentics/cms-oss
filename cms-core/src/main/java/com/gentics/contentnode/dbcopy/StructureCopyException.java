/*
 * @author norbert
 * @date 28.01.2008
 * @version $Id: StructureCopyException.java,v 1.1 2008-02-06 10:55:41 norbert Exp $
 */
package com.gentics.contentnode.dbcopy;

/**
 * Exception that might occur when copying a structure
 */
public class StructureCopyException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2644561119163143312L;

	/**
	 * 
	 */
	public StructureCopyException() {}

	/**
	 * @param message
	 */
	public StructureCopyException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public StructureCopyException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public StructureCopyException(String message, Throwable cause) {
		super(message, cause);
	}
}
