package com.gentics.contentnode.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

@Provider
public class WebApplicationExceptionMapper extends AbstractExceptionMapper implements ExceptionMapper<WebApplicationException> {

	@Override
	public Response toResponse(WebApplicationException exception) {
		tryRollback(exception);

		Response response = exception.getResponse();

		GenericResponse genericResponse = new GenericResponse(new Message(
				Message.Type.CRITICAL, exception.getLocalizedMessage()),
				new ResponseInfo(ResponseCode.FAILURE, exception.getMessage()));

		return generate(genericResponse, Response.Status.fromStatusCode(response.getStatus()));
	}
}
