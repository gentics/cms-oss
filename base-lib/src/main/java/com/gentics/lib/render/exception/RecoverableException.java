/*
 * @author herbert
 * @date 19.04.2007
 * @version $Id: RecoverableException.java,v 1.1 2007-04-27 10:28:51 herbert Exp $
 */
package com.gentics.lib.render.exception;

import com.gentics.api.lib.exception.NodeException;

/**
 * Thrown when a "recoverable" exception happens during a publish run.
 * This means that the current page should not be changed in the publish 
 * table but the general publish run should not fail.
 */
public class RecoverableException extends NodeException {
	private static final long serialVersionUID = 99885877532948265L;

	public RecoverableException() {
		super();
	}

	public RecoverableException(String message) {
		super(message);
	}

	public RecoverableException(String message, Throwable cause) {
		super(message, cause);
	}

	public RecoverableException(Throwable cause) {
		super(cause);
	}

}
