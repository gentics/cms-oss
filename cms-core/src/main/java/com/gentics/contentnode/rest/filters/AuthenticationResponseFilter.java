package com.gentics.contentnode.rest.filters;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import com.gentics.contentnode.etc.ContentNodeHelper;

/**
 * Filter implementation that removes the session from the ContentNodeHelper (after an authenticated request was performed)
 */
@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationResponseFilter implements ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		ContentNodeHelper.setSession(null);
	}
}
