package com.gentics.contentnode.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * This provider maps a {@link ReadOnlyException} to a
 * {@link GenericResponse} containing an appropriate message.
 */
@Provider
public class ReadOnlyMapper extends AbstractExceptionMapper implements ExceptionMapper<ReadOnlyException> {
	@Override
	public Response toResponse(ReadOnlyException exception) {
		tryRollback(exception);

		GenericResponse response = new GenericResponse(new Message(
				Message.Type.CRITICAL, exception.getLocalizedMessage()),
				new ResponseInfo(ResponseCode.PERMISSION, exception.getMessage()));

		return generate(response, Response.Status.CONFLICT);
	}
}
