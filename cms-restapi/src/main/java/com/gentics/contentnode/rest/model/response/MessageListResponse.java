package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing a list of Inbox Messages
 */
@XmlRootElement
public class MessageListResponse extends GenericResponse {

	/**
	 * Create an empty instance
	 */
	public MessageListResponse() {}

	/**
	 * Create an instance with a response message and response info
	 * @param message response message
	 * @param responseInfo
	 */
	public MessageListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}
}
