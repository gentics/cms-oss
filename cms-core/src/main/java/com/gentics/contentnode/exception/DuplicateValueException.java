package com.gentics.contentnode.exception;

import javax.ws.rs.core.Response.Status;

import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;

/**
 * Exception that is thrown, when an entity cannot be modified due to a uniqueness constraint
 */
public class DuplicateValueException extends RestMappedException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 240463103634754817L;

	/**
	 * Create instance
	 * @param field name of the field with the uniqueness constraint
	 * @param value duplicate value
	 */
	public DuplicateValueException(String field, String value) {
		super(I18NHelper.get("exception.duplicate.value", I18NHelper.get(field), value));
		setMessageType(Message.Type.WARNING);
		setResponseCode(ResponseCode.INVALIDDATA);
		setStatus(Status.CONFLICT);
	}
}
