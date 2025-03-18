package com.gentics.contentnode.exception;

import jakarta.ws.rs.core.Response.Status;

import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;

/**
 * Exception that is thrown, when an entity cannot be modified due to a missing required field
 */
public class MissingFieldException extends RestMappedException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3247245203537441785L;

	/**
	 * Create exception with message for the given field
	 * @param field field
	 */
	public MissingFieldException(String field) {
		super(I18NHelper.get("exception.missing.field", I18NHelper.get(field)));
		setMessageType(Message.Type.WARNING);
		setResponseCode(ResponseCode.INVALIDDATA);
		setStatus(Status.BAD_REQUEST);
	}
}
