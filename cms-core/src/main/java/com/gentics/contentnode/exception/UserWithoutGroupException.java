package com.gentics.contentnode.exception;

import javax.ws.rs.core.Response.Status;

import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;

/**
 * Exception that is thrown, when a user would lose the last group assignment
 */
public class UserWithoutGroupException extends RestMappedException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5497056862643011280L;

	/**
	 * Create instance
	 * @param reason message
	 */
	public UserWithoutGroupException(String reason) {
		super(reason);
		setMessageType(Message.Type.WARNING);
		setResponseCode(ResponseCode.INVALIDDATA);
		setStatus(Status.CONFLICT);
	}
}
