package com.gentics.contentnode.factory.object;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Exception that is thrown when a {@link NodeObject} cannot be changed (caused by a property)
 */
public class ObjectModificationException extends NodeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5812757022032299937L;

	/**
	 * Property, that cause the modification exception
	 */
	protected String property;

	/**
	 * Create an instance
	 * @param property property that caused the exception
	 * @param message message
	 * @param messageKey key of the i18n message
	 */
	public ObjectModificationException(String property, String message, String messageKey) {
		super(message, messageKey);
		this.property = property;
	}

	/**
	 * Create an instance
	 * @param property property that caused the exception
	 * @param message message
	 * @param messageKey key of the i18n message
	 * @param parameter single parameter in the i18n message
	 */
	public ObjectModificationException(String property, String message, String messageKey, String parameter) {
		super(message, messageKey, parameter);
		this.property = property;
	}

	/**
	 * Create an instance
	 * @param property property that caused the exception
	 * @param message message
	 * @param messageKey key of the i18n message
	 * @param parameters list of parameters in the i18n message
	 */
	public ObjectModificationException(String property, String message, String messageKey, List<String> parameters) {
		super(message, messageKey, parameters);
		this.property = property;
	}

	/**
	 * Get the name of the property, that caused the exception
	 * @return name of the property
	 */
	public String getProperty() {
		return property;
	}
}
