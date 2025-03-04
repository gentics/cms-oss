package com.gentics.contentnode.rest.resource.impl.internal;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.gentics.contentnode.security.AccessControlService;

/**
 * Abstract base class for internal REST API Resources
 */
@Produces({ MediaType.APPLICATION_JSON })
public abstract class InternalResource {
	/**
	 * Access control 
	 */
	protected static AccessControlService accessControlService = new AccessControlService("javaparserinvoker");

	/**
	 * Request object
	 */
	@Context
	private HttpServletRequest request;

	/**
	 * Response object
	 */
	@Context
	private HttpServletResponse response;

	@PostConstruct
	public void initialize() {
		if (!accessControlService.verifyAccess(request, null)) {
			throw new WebApplicationException(Response.status(Status.FORBIDDEN).build());
		}
	}

	/**
	 * Get the servlet request
	 * @return servlet request
	 */
	public HttpServletRequest getRequest() {
		return request;
	}

	/**
	 * Get the servlet response
	 * @return servlet response
	 */
	public HttpServletResponse getResponse() {
		return response;
	}
}
