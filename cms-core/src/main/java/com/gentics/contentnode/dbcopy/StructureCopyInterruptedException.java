/*
 * @author alexander
 * @date 08.04.2008
 * @version $Id: StructureCopyInterruptedException.java,v 1.2 2008-05-26 15:05:54 norbert Exp $
 */
package com.gentics.contentnode.dbcopy;

public class StructureCopyInterruptedException extends StructureCopyException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 504600686916299324L;

	/**
	 * 
	 */
	public StructureCopyInterruptedException() {}

	/**
	 * @param message
	 */
	public StructureCopyInterruptedException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public StructureCopyInterruptedException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public StructureCopyInterruptedException(String message, Throwable cause) {
		super(message, cause);
	}

}
