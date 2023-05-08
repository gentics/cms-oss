package com.gentics.contentnode.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.gentics.contentnode.exception.RestMappedException;

/**
 * Exception mapper for all subclasses of {@link RestMappedException}
 */
public class RestExceptionMapper extends AbstractExceptionMapper implements ExceptionMapper<RestMappedException> {

	@Override
	public Response toResponse(RestMappedException exception) {
		tryRollback(exception);

		return generate(exception.getRestResponse(), exception.getStatus());
	}
}
