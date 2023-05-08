package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception that is used for incorrect filesizes
 */
public class FileSizeException extends NodeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2672035061434591624L;

	/**
	 * Creates a FileSizeException with a message.
	 * 
	 * @param message Message why the file size is not ok
	 */
	public FileSizeException(String message) {
		super(message);
	}
}
