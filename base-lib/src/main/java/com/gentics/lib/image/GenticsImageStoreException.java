package com.gentics.lib.image;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception, that is thrown by the {@link GenticsImageStore} when the request to the original image returns with a status code other than 200
 */
public class GenticsImageStoreException extends NodeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -288459278935860967L;

	/**
	 * Status Code
	 */
	protected int statusCode;

	/**
	 * Create instance with given message and status code
	 * @param message message
	 * @param statusCode status code
	 */
	public GenticsImageStoreException(String message, int statusCode) {
		super(message);
		this.statusCode = statusCode;
	}

	/**
	 * Get the status code
	 * @return status code
	 */
	public int getStatusCode() {
		return statusCode;
	}
}
