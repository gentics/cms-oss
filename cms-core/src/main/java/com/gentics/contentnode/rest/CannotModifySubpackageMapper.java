package com.gentics.contentnode.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.gentics.contentnode.rest.exceptions.CannotModifySubpackageException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * This provider maps a {@link CannotModifySubpackageException} to a
 * {@link GenericResponse} containing an appropriate message.
 */
@Provider
public class CannotModifySubpackageMapper extends AbstractExceptionMapper implements ExceptionMapper<CannotModifySubpackageException> {
	@Override
	public Response toResponse(CannotModifySubpackageException exception) {
		tryRollback(exception);

		GenericResponse response = new GenericResponse(new Message(
				Message.Type.WARNING, exception.getLocalizedMessage()),
				new ResponseInfo(ResponseCode.FAILURE, exception.getMessage()));

		return generate(response, Response.Status.CONFLICT);
	}
}
