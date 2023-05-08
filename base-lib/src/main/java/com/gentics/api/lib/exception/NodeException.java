/*
 * @author unknown
 * @date unknown
 * @version $Id: NodeException.java,v 1.2.32.1 2011-03-15 14:02:04 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.exception;

import java.util.Arrays;
import java.util.List;

import com.gentics.lib.i18n.CNI18nString;

/**
 * General exception thrown in .node
 */
public class NodeException extends Exception {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 499920272991261812L;

	/**
	 * Message key for the localized message.
	 */
	private String messageKey = null;
    
	/**
	 * List of parameters for the localized message.
	 */
	private List<String> parameters = null;

	/**
	 * Create an instance of the exception
	 */
	public NodeException() {
		super();
	}

	/**
	 * Create an instance of the exception with a message
	 * @param message message of the exception
	 */
	public NodeException(String message) {
		super(message);
	}

	/**
	 * Create an instance of the exception with a message and a cause
	 * @param message message of the exception
	 * @param cause cause of the exception
	 */
	public NodeException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create an instance of the exception with a cause
	 * @param cause cause of the exception
	 */
	public NodeException(Throwable cause) {
		super(cause);
	}

	/**
	 * Create an instance of the exception with a message and a localized message.
	 * @param message A plain text message that contains information about the error.
	 * @param messageKey A message key that is used to construct a CNI18nString for the localized message.
	 */
	public NodeException(String message, String messageKey) {
		this(message, messageKey, (List<String>) null);
	}

	/**
	 * Create an instance of the exception with a message and a localized message.
	 * @param message A plain text message that contains information about the error.
	 * @param messageKey A message key that is used to construct a CNI18nString for the localized message.
	 * @param parameters List of parameters for the localized message.
	 */
	public NodeException(String message, String messageKey, List<String> parameters) {
		super(message);
		this.messageKey = messageKey;
		this.parameters = parameters;
	}

	/**
	 * Create an instance of the exception with a message and a localized message. 
	 * @param message A plain text message that contains information about the error.
	 * @param messageKey A message key that is used to construct a CNI18nString for the localized message.
	 * @param parameter Single parameter for the localized message. If you need more than one parameter use {@link InsufficientPrivilegesException#InsufficientPrivilegesException(String, String, List)}.
	 */
	public NodeException(String message, String messageKey, String parameter) {
		super(message);
		this.messageKey = messageKey;
		this.parameters = Arrays.asList(new String[] { parameter});
	}

	/**
	 * Returns the localized message from the given message key.
	 * If no messageKey was provided, the standard message is returned.
	 */
	public String getLocalizedMessage() {
		if (messageKey != null) {
			CNI18nString message = new CNI18nString(messageKey);

			if (parameters != null) {
				message.setParameters(parameters);
			}
			return message.toString();
		} else {
			return this.getMessage();
		}
	}
}
