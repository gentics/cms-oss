/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: GenericResponse.java,v 1.2.6.1 2011-03-16 13:32:37 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Generic response containing the response code and response messages (no additional objects)
 * @author norbert
 */
@XmlRootElement
public class GenericResponse implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4897796445376071865L;

	/**
	 * List of messages displayed to the user
	 */
	private List<Message> messages;

	/**
	 * Response responseInfo for this response
	 */
	private ResponseInfo responseInfo;

	/**
	 * Flag to mark responses for jobs that are continued in the background
	 */
	private boolean inBackground;

	/**
	 * Constructor used by JAXB
	 */
	public GenericResponse() {}

	/**
	 * Creates a GenericResponse with the provided single message and ResponseInfo.
	 * 
	 * @param message The message that should be displayed to the user
	 * @param response ResponseInfo with the status of the response
	 */
	public GenericResponse(Message message, ResponseInfo responseInfo) {
		this.messages = new LinkedList<Message>();
		if (message != null) {
			this.messages.add(message);
		}

		this.responseInfo = responseInfo;
	}

	/**
	 * Messages contained in the response (which should be shown to the user)
	 * @documentationType java.util.List
	 * @return list of messages
	 */
	public List<Message> getMessages() {
		return messages;
	}

	public void addMessage(Message message) {
		if (messages == null) {
			messages = new LinkedList<Message>();
		}
		messages.add(message);
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	public ResponseInfo getResponseInfo() {
		return responseInfo;
	}

	public void setResponseInfo(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
	}

	/**
	 * Whether the job was pushed into the background.
	 * @return Whether the job was pushed into the background.
	 */
	public boolean isInBackground() {
		return inBackground;
	}

	/**
	 * Set whether the job was pushed into the background.
	 * @param inBackground Whether the job was pushed into the background.
	 * @return Fluent API
	 */
	public GenericResponse setInBackground(boolean inBackground) {
		this.inBackground = inBackground;

		return this;
	}
}
