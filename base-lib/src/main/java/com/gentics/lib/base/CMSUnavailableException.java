/*
 * @author unknown
 * @date unknown
 * @version $Id: CMSUnavailableException.java,v 1.5 2006-01-23 16:40:51 norbert Exp $
 */
package com.gentics.lib.base;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception that is thrown when the cms is unavailable
 * @author norbert
 */
public class CMSUnavailableException extends NodeException {
	public CMSUnavailableException() {
		super();
	}

	public CMSUnavailableException(String message) {
		super(message);
	}

	public CMSUnavailableException(String message, Exception nested) {
		super(message, nested);
	}

}
