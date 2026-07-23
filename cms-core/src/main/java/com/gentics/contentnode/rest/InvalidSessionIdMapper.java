package com.gentics.contentnode.rest;

import com.gentics.contentnode.factory.InvalidSessionIdException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * This provider handles an {@link InvalidSessionIdException}
 */
@Provider
public class InvalidSessionIdMapper extends AbstractExceptionMapper
		implements ExceptionMapper<InvalidSessionIdException> {
	public Response toResponse(InvalidSessionIdException ex) {
		tryRollback(ex, false);

		return Response.status(Response.Status.UNAUTHORIZED).entity("").build();
	}

}
