package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request to send messages to users/groups
 */
@XmlRootElement
public class MessageSendRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -9025670841265322464L;

	/**
	 * Message to be sent. This may be a i18n key.
	 */
	private String message;

	/**
	 * Translated messages
	 */
	private Map<String, String> translations;

	/**
	 * Optional list of parameters, that are filled into the message (if the message is an i18n key)
	 */
	private List<String> parameters;

	/**
	 * List of users who should get the message
	 */
	private List<Integer> toUserId;

	/**
	 * List of groups which should get the message
	 */
	private List<Integer> toGroupId;

	/**
	 * The number of minutes a message is considered as instant message
	 */
	private int instantTimeMinutes;

	/**
	 * Create an empty instance
	 */
	public MessageSendRequest() {}

	/**
	 * Message to be sent. This may be an i18n key.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Optional map of translations per language code. If a translation for the adressees language is found here, it will be used.
	 * @return translations per language code
	 */
	public Map<String, String> getTranslations() {
		return translations;
	}

	/**
	 * Optional list of parameters, that are filled into the message (if the message is an i18n key)
	 * @return list of parameters
	 */
	public List<String> getParameters() {
		return parameters;
	}

	/**
	 * List of group IDs to send the message
	 * @return list of group IDs
	 */
	public List<Integer> getToGroupId() {
		return toGroupId;
	}

	/**
	 * List of user IDs to send the message
	 * @return
	 */
	public List<Integer> getToUserId() {
		return toUserId;
	}

	/**
	 * Gets the value of the instantTimeMinutes field.
	 * @return The value of the instantTimeMinutes field.
	 */
	public int getInstantTimeMinutes() {
		return this.instantTimeMinutes;
	}

	/**
	 * Sets the value of the instantTimeMinutes field.
	 * @param instantTimeMinutes The value to set the instantTimeMinutes field to.
	 */
	public void setInstantTimeMinutes(int instantTimeMinutes) {
		this.instantTimeMinutes = instantTimeMinutes;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setTranslations(Map<String, String> translations) {
		this.translations = translations;
	}

	public void setParameters(List<String> parameters) {
		this.parameters = parameters;
	}

	public void setToGroupId(List<Integer> toGroupId) {
		this.toGroupId = toGroupId;
	}

	public void setToUserId(List<Integer> toUserId) {
		this.toUserId = toUserId;
	}
}
