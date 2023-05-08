package com.gentics.contentnode.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.gentics.contentnode.rest.exceptions.DuplicateEntityException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * This provider maps a {@link DuplicateEntityException} to a
 * {@link GenericResponse} containing an appropriate message.
 */
@Provider
public class DuplicateEntityMapper extends AbstractExceptionMapper implements ExceptionMapper<DuplicateEntityException> {
	@Override
	public Response toResponse(DuplicateEntityException exception) {
		tryRollback(exception);

		GenericResponse response = new GenericResponse(new Message(
				Message.Type.WARNING, exception.getLocalizedMessage()),
				new ResponseInfo(ResponseCode.FAILURE, exception.getMessage()));

		return generate(response, Response.Status.CONFLICT);
	}
}
