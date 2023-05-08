package com.gentics.contentnode.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonParseException;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * This provider handles Json Parsing Problems by sending a response with an appropriate message
 */
@Provider
public class JsonParseExceptionMapper extends AbstractExceptionMapper implements ExceptionMapper<JsonParseException> {
	@Override
	public Response toResponse(JsonParseException exception) {
		tryRollback(exception);

		GenericResponse response = new GenericResponse(new Message(
				Message.Type.CRITICAL, I18NHelper.get("rest.invalid.json.error")),
				new ResponseInfo(ResponseCode.INVALIDDATA, exception.getMessage()));

		return generate(response, Response.Status.BAD_REQUEST);
	}
}
