/*
 * @author norbert
 * @date 14.10.2010
 * @version $Id: EntityNotFoundException.java,v 1.1.6.1 2011-03-15 14:02:03 norbert Exp $
 */
package com.gentics.contentnode.rest.exceptions;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception is thrown when a requested entity was not found
 */
public class EntityNotFoundException extends NodeException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7960907672888654099L;

	/**
	 * 
	 */
	public EntityNotFoundException() {}

	/**
	 * @param message
	 */
	public EntityNotFoundException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public EntityNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause
	 */
	public EntityNotFoundException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param messageKey
	 * @param parameters
	 */
	public EntityNotFoundException(String message, String messageKey, List<String> parameters) {
		super(message, messageKey, parameters);
	}

	/**
	 * @param message
	 * @param messageKey
	 * @param parameter
	 */
	public EntityNotFoundException(String message, String messageKey, String parameter) {
		super(message, messageKey, parameter);
	}

	/**
	 * @param message
	 * @param messageKey
	 */
	public EntityNotFoundException(String message, String messageKey) {
		super(message, messageKey);
	}
}
