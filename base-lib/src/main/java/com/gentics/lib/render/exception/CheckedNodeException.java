/*
 * @author herbert
 * @date 27.04.2007
 * @version $Id: CheckedNodeException.java,v 1.1 2007-04-27 10:28:51 herbert Exp $
 */
package com.gentics.lib.render.exception;

import com.gentics.api.lib.exception.NodeException;

public class CheckedNodeException extends NodeException {
	private static final long serialVersionUID = 1L;

	public CheckedNodeException() {
		super();
	}

	public CheckedNodeException(String message) {
		super(message);
	}

	public CheckedNodeException(String message, Throwable cause) {
		super(message, cause);
	}

	public CheckedNodeException(Throwable cause) {
		super(cause);
	}

}
