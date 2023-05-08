/*
 * @author herbert
 * @date 15.03.2007
 * @version $Id: PublishException.java,v 1.1 2007-04-27 10:28:51 herbert Exp $
 */
package com.gentics.lib.render.exception;

import com.gentics.api.lib.exception.NodeException;

/**
 * Generic exceptions happening during publish/writefs run.
 * 
 * @author herbert
 */
public class PublishException extends NodeException {
	private static final long serialVersionUID = -3723261480939665681L;

	public PublishException() {
		super();
	}

	public PublishException(String message, Throwable cause) {
		super(message, cause);
	}

	public PublishException(String message) {
		super(message);
	}

	public PublishException(Throwable cause) {
		super(cause);
	}

}
