package com.gentics.contentnode.rest.filters;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Filter for checking access control. Will be used for resources/methods, which are annotated with {@link AccessControl}.
 */
@Provider
@AccessControl("")
@Priority(Priorities.AUTHENTICATION)
public class AccessControlFilter implements ContainerRequestFilter {
	@Context
	ResourceInfo resourceInfo;

	@Context
	private HttpServletRequest httpServletRequest;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		NodeConfigRuntimeConfiguration config = NodeConfigRuntimeConfiguration.getDefault();
		NodePreferences prefs = config.getNodeConfig().getDefaultPreferences();

		Set<String> allowedHosts = new HashSet<>();
		if (resourceInfo.getResourceMethod() != null) {
			allowedHosts.addAll(getAllowedHosts(prefs, resourceInfo.getResourceMethod().getAnnotationsByType(AccessControl.class)));
		}
		if (resourceInfo.getResourceClass() != null) {
			allowedHosts.addAll(getAllowedHosts(prefs, resourceInfo.getResourceClass().getAnnotationsByType(AccessControl.class)));
		}

		if (!allowedHosts.contains("ALL - YES, I know what I do !!")) {
			// check allowed host
			if (httpServletRequest != null) {
				if (!com.gentics.contentnode.security.AccessControlService.isIpAddressInList(httpServletRequest.getRemoteAddr(), allowedHosts)
						&& !com.gentics.contentnode.security.AccessControlService.isHostInList(httpServletRequest.getRemoteHost(), allowedHosts)) {
					requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity("").build());
				}
			}
		}

	}

	protected Set<String> getAllowedHosts(NodePreferences prefs, AccessControl[] annotations) {
		Set<String> hostNames = new HashSet<>();
		for (AccessControl annotation : annotations) {
			String[] value = prefs.getProperties("" + annotation.value().toLowerCase());
			if (value != null) {
				hostNames.addAll(Arrays.asList(value));
			}
		}
		return hostNames;
	}
}
