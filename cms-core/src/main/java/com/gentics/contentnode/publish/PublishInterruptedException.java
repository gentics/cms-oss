/*
 * @author herbert
 * @date 19.04.2007
 * @version $Id: PublishInterruptedException.java,v 1.2 2007-04-27 10:28:51 herbert Exp $
 */
package com.gentics.contentnode.publish;

import com.gentics.lib.render.exception.PublishException;

/**
 * This exception is thrown if during a publish run it is detected
 * that the thread as interrupted - usually because the user wanted
 * to stop the publish run.
 */
public class PublishInterruptedException extends PublishException {
	private static final long serialVersionUID = 692024581504444366L;

	public PublishInterruptedException() {
		super();
	}

	public PublishInterruptedException(String message) {
		super(message);
	}

	public PublishInterruptedException(String message, Throwable cause) {
		super(message, cause);
	}

	public PublishInterruptedException(Throwable cause) {
		super(cause);
	}

}
