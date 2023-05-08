package com.gentics.api.contentnode.migration;

/**
 * Exception that may be thrown in implementations of {@link IMigrationPostprocessor}
 * 
 * @author Taylor
 */
public class MigrationException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4348565070187977316L;

	/**
	 * Creates an instance of the exception without message or cause
	 */
	public MigrationException() {}

	/**
	 * Creates an instance of the exception with the given message
	 * 
	 * @param message
	 *            exception message
	 */
	public MigrationException(String message) {
		super(message);
	}

	/**
	 * Creates an instance of the exception with the given cause
	 * 
	 * @param cause
	 *            exception cause
	 */
	public MigrationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates an instance of the exception with the given message and cause
	 * 
	 * @param message
	 *            exception message
	 * @param cause
	 *            exception cause
	 */
	public MigrationException(String message, Throwable cause) {
		super(message, cause);
	}

}
