package com.gentics.contentnode.exception;

import java.util.List;

import javax.ws.rs.core.Response;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Generic Mapped exception. Subclasses can define the response status, response code and message
 * to be sent to the requestor
 */
public class RestMappedException extends NodeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 7230813368801649937L;

	/**
	 * Response status
	 */
	protected Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;

	/**
	 * Response code
	 */
	protected ResponseCode responseCode = ResponseCode.FAILURE;

	/**
	 * Message type in the response
	 */
	protected Message.Type messageType = Message.Type.CRITICAL;

	/**
	 * Optional response
	 */
	protected GenericResponse response;

	/**
	 * Create default instance
	 */
	public RestMappedException() {
		super();
	}

	/**
	 * Create instance with GenericResponse
	 * @param response response
	 */
	public RestMappedException(GenericResponse response) {
		super();
		this.response = response;
	}

	/**
	 * Create instance
	 * @param message response message
	 * @param messageKey i18n key of the translatable user message
	 * @param parameters parameters in the user message
	 */
	public RestMappedException(String message, String messageKey, List<String> parameters) {
		super(message, messageKey, parameters);
	}

	/**
	 * Create instance
	 * @param message response message
	 * @param messageKey i18n key of the translatable user message
	 * @param parameter parameter in the user message
	 */
	public RestMappedException(String message, String messageKey, String parameter) {
		super(message, messageKey, parameter);
	}

	/**
	 * Create instance
	 * @param message response message
	 * @param messageKey i18n key of the translatable user message
	 */
	public RestMappedException(String message, String messageKey) {
		super(message, messageKey);
	}

	/**
	 * Create instance
	 * @param message response message
	 * @param cause cause
	 */
	public RestMappedException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create instance
	 * @param message response message
	 */
	public RestMappedException(String message) {
		super(message);
	}

	/**
	 * Create instance
	 * @param cause cause
	 */
	public RestMappedException(Throwable cause) {
		super(cause);
	}

	/**
	 * Get response status
	 * @return response status
	 */
	public Response.Status getStatus() {
		return status;
	}

	/**
	 * Set the response status
	 * @param status response status
	 * @return fluent API
	 */
	public RestMappedException setStatus(Response.Status status) {
		this.status = status;
		return this;
	}

	/**
	 * Get response code
	 * @return response code
	 */
	public ResponseCode getResponseCode() {
		return responseCode;
	}

	/**
	 * Set response code
	 * @param responseCode response code
	 * @return fluent API
	 */
	public RestMappedException setResponseCode(ResponseCode responseCode) {
		this.responseCode = responseCode;
		return this;
	}

	/**
	 * Get message type
	 * @return message type
	 */
	public Message.Type getMessageType() {
		return messageType;
	}

	/**
	 * Set message type
	 * @param messageType message type
	 * @return fluent API
	 */
	public RestMappedException setMessageType(Message.Type messageType) {
		this.messageType = messageType;
		return this;
	}

	/**
	 * Get the response
	 * @return response
	 */
	public GenericResponse getRestResponse() {
		if (response != null) {
			return response;
		} else {
			return new GenericResponse(new Message(messageType, getLocalizedMessage()), new ResponseInfo(responseCode, getMessage()));
		}
	}
}
