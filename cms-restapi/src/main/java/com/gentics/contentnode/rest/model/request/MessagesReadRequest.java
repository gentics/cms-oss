package com.gentics.contentnode.rest.model.request;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to set messages to be read
 */
@XmlRootElement
public class MessagesReadRequest {

	/**
	 * List of messages set to be read
	 */
	protected List<Integer> messages;

	/**
	 * Create a new instance
	 */
	public MessagesReadRequest() {}

	/**
	 * Set the list of message ids
	 * @param messages list of message ids
	 */
	public void setMessages(List<Integer> messages) {
		this.messages = messages;
	}

	/**
	 * Get the list of message ids
	 * @return list of message ids
	 */
	public List<Integer> getMessages() {
		return messages;
	}
}
