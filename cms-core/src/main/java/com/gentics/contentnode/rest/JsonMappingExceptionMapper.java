package com.gentics.contentnode.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * This provider handles Json Mapping Problems by sending a response with an appropriate message
 */
@Provider
public class JsonMappingExceptionMapper extends AbstractExceptionMapper implements ExceptionMapper<JsonMappingException> {
	@Override
	public Response toResponse(JsonMappingException exception) {
		tryRollback(exception);

		GenericResponse response = new GenericResponse(new Message(
				Message.Type.CRITICAL, I18NHelper.get("rest.incorrect.body.error")),
				new ResponseInfo(ResponseCode.INVALIDDATA, exception.getMessage()));

		return generate(response, Response.Status.BAD_REQUEST);
	}
}
