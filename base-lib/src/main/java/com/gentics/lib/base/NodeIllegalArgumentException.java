package com.gentics.lib.base;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception thrown if an illegal argument was passed to a method.
 */
public class NodeIllegalArgumentException extends NodeException {
	public NodeIllegalArgumentException() {
		super();
	}

	public NodeIllegalArgumentException(String msg) {
		super(msg);
	}

	public NodeIllegalArgumentException(String msg, Exception inner) {
		super(msg, inner);
	}

}
