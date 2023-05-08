/*
 * @author norbert
 * @date 14.10.2010
 * @version $Id: EntityNotFoundMapper.java,v 1.1 2010-10-14 13:58:48 norbert Exp $
 */
package com.gentics.contentnode.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * This provider maps a {@link EntityNotFoundException} to a
 * {@link GenericResponse} containing an appropriate message.
 */
@Provider
public class EntityNotFoundMapper extends AbstractExceptionMapper implements
		ExceptionMapper<EntityNotFoundException> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
	 */
	public Response toResponse(EntityNotFoundException ex) {
		tryRollback(ex);

		GenericResponse response = new GenericResponse(new Message(
				Message.Type.WARNING, ex.getLocalizedMessage()),
				new ResponseInfo(ResponseCode.NOTFOUND, ex.getMessage()));

		return generate(response, Response.Status.NOT_FOUND);
	}
}
