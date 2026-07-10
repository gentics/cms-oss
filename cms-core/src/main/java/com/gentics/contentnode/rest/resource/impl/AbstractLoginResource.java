package com.gentics.contentnode.rest.resource.impl;

import com.gentics.contentnode.etc.LoginService;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.factory.ContentNodeFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

/**
 * Base class for login resources with a reference to the respective server loader.
 */
public abstract class AbstractLoginResource {
	/**
	 * Service loader that finds implementations of the LoginService interface
	 */
	protected final static ServiceLoaderUtil<LoginService> LOGIN_SERVICE_LOADER = ServiceLoaderUtil.load(LoginService.class);

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

	private ContentNodeFactory factory = ContentNodeFactory.getInstance();

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

	/**
	 * Get the ContentNodeFactory
	 * @return The ContentNodeFactory
	 */
	public ContentNodeFactory getFactory() {
		return factory;
	}
}
