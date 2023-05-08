package com.gentics.lib.genericexceptions;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;

/**
 * @author laurin common class or interface usage exceptions. like missing
 *         setter calls, etc.
 */
public class IllegalUsageException extends NodeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8309574100329636011L;

	/**
	 * @see NodeException#NodeException(String, String, List)
	 */
	public IllegalUsageException(String message, String messageKey, List<String> parameters) {
		super(message, messageKey, parameters);
	}

	/**
	 * @see NodeException#NodeException(String, String, String)
	 */
	public IllegalUsageException(String message, String messageKey, String parameter) {
		super(message, messageKey, parameter);
	}

	/**
	 * @see NodeException#NodeException(String, String)
	 */
	public IllegalUsageException(String message, String messageKey) {
		super(message, messageKey);
	}

	/**
	 * @see NodeException#NodeException()
	 */
	public IllegalUsageException() {
		super();
	}

	/**
	 * @see NodeException#NodeException(String)
	 */
	public IllegalUsageException(String arg0) {
		super(arg0);
	}

	/**
	 * @see NodeException#NodeException(String, Throwable)
	 */
	public IllegalUsageException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * @see NodeException#NodeException(Throwable)
	 */
	public IllegalUsageException(Throwable arg0) {
		super(arg0);
	}
}
