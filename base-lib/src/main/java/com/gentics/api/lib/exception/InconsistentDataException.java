/*
 * @author herbert
 * @date Jun 27, 2007
 * @version $Id: InconsistentDataException.java,v 1.2 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.api.lib.exception;

public class InconsistentDataException extends NodeException {
	private static final long serialVersionUID = 1L;

	public InconsistentDataException(String message, Throwable cause) {
		super(message, cause);
	}
}
