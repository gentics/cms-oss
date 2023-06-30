package com.gentics.contentnode.rest.resource.impl.internal;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.ws.spi.http.HttpContext;

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

	/**
	 * Jersey HTTP Context
	 */
	@Context
	private HttpContext context;

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
