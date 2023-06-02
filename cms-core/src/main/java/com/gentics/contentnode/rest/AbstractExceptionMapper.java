package com.gentics.contentnode.rest;

import java.util.Arrays;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.lib.log.NodeLogger;

public abstract class AbstractExceptionMapper {
	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(AbstractExceptionMapper.class);

	@Context
	protected HttpHeaders headers;

	/**
	 * Check whether the given media type can be accepted by the acceptable
	 * media types from the current request.
	 *
	 * @param type
	 * @return
	 */
	protected boolean clientAccepts(MediaType type) {
		for (MediaType acceptedMediaType : headers.getAcceptableMediaTypes()) {
			if (acceptedMediaType.isCompatible(type)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Try to rollback the current transaction.
	 * 
	 * @param e
	 *            root cause exception
	 */
	protected void tryRollback(Exception e) {
		logger.error("Error in request", e);
		Transaction t;
		t = TransactionManager.getCurrentTransactionOrNull();
		if (t != null) {
			try {
				t.rollback(false);
			} catch (TransactionException e1) {
			}
		} else if (logger.isDebugEnabled()) {
			logger.debug("Could not get transaction for rollback.");
		}
	}

	/**
	 * Generate the response to be sent to the client
	 * @param response GenericResponse instance
	 * @param status http reponse status
	 * @return response to send
	 */
	protected Response generate(GenericResponse response, Response.Status status) {
		for (MediaType type : Arrays.asList(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE)) {
			if (clientAccepts(type)) {
				return Response.status(status).entity(response).type(type).build();
			}
		}

		//TODO use ExceptionUtils.getFullStackTrace(e) or redirect to custom error page
		return Response.status(status).entity("ERROR").type(MediaType.TEXT_PLAIN).build();
	}
}
