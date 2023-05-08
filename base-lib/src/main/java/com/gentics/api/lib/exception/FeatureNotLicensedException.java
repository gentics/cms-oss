package com.gentics.api.lib.exception;

import java.util.List;

public class FeatureNotLicensedException extends NodeException {

	private static final long serialVersionUID = 5861904829569425234L;

	/**
	 * Create an instance of the exception
	 */
	public FeatureNotLicensedException() {
		super();
	}

	/**
	 * Create an instance with message, message key for localized message and parameters
	 * 
	 * @param message
	 *            internal message
	 * @param messageKey
	 *            localized message key
	 * @param parameters
	 *            parameters for the localized message
	 */
	public FeatureNotLicensedException(String message, String messageKey, List<String> parameters) {
		super(message, messageKey, parameters);
	}

	/**
	 * Create an instance with message and localized message
	 * 
	 * @param message
	 *            internal message
	 * @param messageKey
	 *            localized message key
	 * @param parameter
	 *            parameter for the localized message
	 */
	public FeatureNotLicensedException(String message, String messageKey, String parameter) {
		super(message, messageKey, parameter);
	}

	/**
	 * Create an instance with message and localized message
	 * 
	 * @param message
	 *            internal message
	 * @param messageKey
	 *            localized message key
	 */
	public FeatureNotLicensedException(String message, String messageKey) {
		super(message, messageKey);
	}

	/**
	 * Create an instance with message and cause
	 * 
	 * @param message
	 *            internal message
	 * @param cause
	 *            cause
	 */
	public FeatureNotLicensedException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create an instance with cause
	 * 
	 * @param cause
	 *            cause
	 */
	public FeatureNotLicensedException(Throwable cause) {
		super(cause);
	}

	/**
	 * Create an instance of the exception with a message
	 * 
	 * @param message
	 *            message of the exception
	 */
	public FeatureNotLicensedException(String message) {
		super(message);
	}

}
