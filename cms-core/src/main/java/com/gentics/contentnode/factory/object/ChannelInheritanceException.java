package com.gentics.contentnode.factory.object;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;

/**
 * This Exception is thrown when a change would introduce an inconsistency into
 * the channel inheritance structure.
 *
 * @author escitalopram
 *
 */
public class ChannelInheritanceException extends NodeException {

	private static final long serialVersionUID = -4997343713579348217L;

	/**
	 * Constructs a new ChannelInheritanceException
	 * @param message the reason why this exception is being thrown
	 */
	public ChannelInheritanceException(String message) {
		super(message);
	}

	/**
	 * Create an instance
	 * @param message internal message
	 * @param messageKey message key for the localized message
	 */
	public ChannelInheritanceException(String message, String messageKey) {
		super(message, messageKey);
	}

	/**
	 * Create an instance
	 * @param message internal message
	 * @param messageKey message key for the localized message
	 * @param parameter single parameter for the localized message
	 */
	public ChannelInheritanceException(String message, String messageKey, String parameter) {
		super(message, messageKey, parameter);
	}

	/**
	 * Create an instance
	 * @param message internal messsage
	 * @param messageKey message key for the localized message
	 * @param parameters list of parameters for the localized message
	 */
	public ChannelInheritanceException(String message, String messageKey, List<String> parameters) {
		super(message, messageKey, parameters);
	}
}
