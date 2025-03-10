package com.gentics.contentnode.exception;

import jakarta.ws.rs.core.Response.Status;

import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;

/**
 * Exception that is thrown, when a resource or entity cannot be deleted or moved, because of being used elsewhere.
 */
public class EntityInUseException extends RestMappedException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3834904103510575365L;

	/**
	 * Create instance
	 * @param message message
	 */
	public EntityInUseException(String message) {
		super(message);
		setMessageType(Message.Type.WARNING);
		setResponseCode(ResponseCode.INVALIDDATA);
		setStatus(Status.CONFLICT);
	}
}
