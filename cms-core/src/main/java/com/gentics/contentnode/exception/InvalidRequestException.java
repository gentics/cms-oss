package com.gentics.contentnode.exception;

import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import jakarta.ws.rs.core.Response.Status;

/**
 * Exception that is thrown, when an invalid request was issued
 */
public class InvalidRequestException extends RestMappedException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7339815097265162857L;

	/**
	 * Create instance
	 * @param message message
	 */
	public InvalidRequestException(String message) {
		super(message);
		setMessageType(Type.CRITICAL);
		setResponseCode(ResponseCode.FAILURE);
		setStatus(Status.BAD_REQUEST);
	}
}
