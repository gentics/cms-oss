/*
 * @author floriangutmann
 * @date Apr 21, 2010
 * @version $Id: ReadOnlyException.java,v 1.1.6.1 2011-03-16 13:32:37 norbert Exp $
 */
package com.gentics.api.lib.exception;

import java.util.List;

/**
 * Exception that is used when a NodeObject is requested for update but the page is only available for read.
 * 
 * @author floriangutmann
 */
public class ReadOnlyException extends NodeException {
    
	/**
	 * Serial version id
	 */
	private static final long serialVersionUID = -960643942616372907L;

	/**
	 * Creates a ReadOnlyException with a message and a cause of the exception.
	 * 
	 * @param message Message why the resource is read only
	 * @param cause Cause of the exception
	 */
	public ReadOnlyException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Empty Constructor
	 */
	public ReadOnlyException() {
		super();
	}

	/**
	 * Create Instance
	 * @param message message
	 * @param messageKey message key for i18n
	 * @param parameters parameters to include in the translated message
	 */
	public ReadOnlyException(String message, String messageKey, List<String> parameters) {
		super(message, messageKey, parameters);
	}

	/**
	 * Create instance
	 * @param message message
	 * @param messageKey message key for i18n
	 * @param parameter single parameter to include in the translated message
	 */
	public ReadOnlyException(String message, String messageKey, String parameter) {
		super(message, messageKey, parameter);
	}

	/**
	 * Create instance
	 * @param message message
	 * @param messageKey message key for i18n
	 */
	public ReadOnlyException(String message, String messageKey) {
		super(message, messageKey);
	}

	/**
	 * Create instance
	 * @param cause causing throwable
	 */
	public ReadOnlyException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a ReadOnlyException with a message.
	 * 
	 * @param message Message why the resource is read only.
	 */
	public ReadOnlyException(String message) {
		super(message);
	}
}
