package com.gentics.contentnode.exception;

import javax.ws.rs.core.Response.Status;

import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;

/**
 * Exception that is thrown, when a group cannot be moved
 */
public class MovingGroupNotPossibleException extends RestMappedException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6621816132651968334L;

	/**
	 * Create instance
	 * @param message message
	 */
	public MovingGroupNotPossibleException(String message) {
		super(message);
		setMessageType(Message.Type.WARNING);
		setResponseCode(ResponseCode.INVALIDDATA);
		setStatus(Status.CONFLICT);
	}
}
