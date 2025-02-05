/*
 * @author norbert
 * @date 14.10.2010
 * @version $Id: InsufficientPrivilegesMapper.java,v 1.1 2010-10-14 13:58:48 norbert Exp $
 */
package com.gentics.contentnode.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * This provider maps a {@link InsufficientPrivilegesException} to a
 * {@link GenericResponse} containing an appropriate message.
 */
@Provider
public class InsufficientPrivilegesMapper extends AbstractExceptionMapper
		implements ExceptionMapper<InsufficientPrivilegesException> {

	/**
	 * Log the {@link InsufficientPrivilegesException} with the {@link ActionLogger#securityLogger} and into logcmd
	 * @param ex exception
	 */
	public static void log(InsufficientPrivilegesException ex) {
		PermType permType = ex.getPermType();
		String msg = String.format("(user: %s, perm: %s) %s", ex.getUserName(), permType != null ? permType.name() : "unknown", ex.getLocalizedMessage());
		ActionLogger.securityLogger.error(msg);
		try (Trx trx = new Trx(null, ex.getUserId())) {
			ActionLogger.logCmd(ActionLogger.ACCESS_DENIED, ex.getObjectType(), ex.getObjectId(), 0, msg);
			trx.success();
		} catch (NodeException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jakarta.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
	 */
	public Response toResponse(InsufficientPrivilegesException ex) {
		log(ex);
		tryRollback(ex);

		GenericResponse response = new GenericResponse(new Message(
				Message.Type.CRITICAL, ex.getLocalizedMessage()),
				new ResponseInfo(ResponseCode.PERMISSION, ex.getMessage()));

		return generate(response, Response.Status.FORBIDDEN);
	}
}
