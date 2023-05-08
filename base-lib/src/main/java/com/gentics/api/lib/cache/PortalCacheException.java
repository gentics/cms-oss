/*
 * @author Clemens Prerovsky
 */
package com.gentics.api.lib.cache;

/**
 * Exception that is thrown if something goes wrong with the {@link PortalCache}.
 */
public class PortalCacheException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7721555498827404714L;

	/**
	 * constructs a new PortalCacheException
	 */
	public PortalCacheException() {
		super();
	}

	/**
	 * constructs a new PortalCacheException with a message
	 * @param message the detail message
	 */
	public PortalCacheException(String message) {
		super(message);
	}

	/**
	 * constructs a new PortalCacheException with it's cause attached
	 * @param cause the cause of this exception
	 */
	public PortalCacheException(Throwable cause) {
		super(cause);
	}

	/**
	 * constructs a new PortalCacheException with a message and it's cause
	 * @param message the detail message
	 * @param cause the cause of this exception
	 */
	public PortalCacheException(String message, Throwable cause) {
		super(message, cause);
	}
}
