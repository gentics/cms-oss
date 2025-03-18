package com.gentics.contentnode.rest.filters;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Container request filter that checks permissions, if resource class or method is annotated with {@link RequiredPerm}.
 */
@Provider
@Authenticated
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationRequestFilter implements ContainerRequestFilter {
	@Context
	ResourceInfo resourceInfo;

	/**
	 * Check whether the current user has all permissions granted, that are annotated at either the given resource method or resource class
	 * @param method resource method (may be null)
	 * @param clazz resource class (may be null)
	 * @throws NodeException
	 * @throws InsufficientPrivilegesException if the permission is not granted
	 */
	public static void check(Method method, Class<?> clazz) throws NodeException, InsufficientPrivilegesException {
		Collection<RequiredPerm> perms = new ArrayList<>();
		if (method != null) {
			perms.addAll(Arrays.asList(method.getAnnotationsByType(RequiredPerm.class)));
		}
		if (clazz != null) {
			perms.addAll(Arrays.asList(clazz.getAnnotationsByType(RequiredPerm.class)));
		}

		if (!perms.isEmpty()) {
			try (Trx trx = ContentNodeHelper.trx()) {
				for (RequiredPerm perm : perms) {
					check(perm);
				}
			}
		}
	}

	/**
	 * Check whether the current user has sufficient permissions.
	 * @param perm perm annotation
	 * @param requestContext request context
	 * @throws NodeException
	 * @throws InsufficientPrivilegesException if the permission is not granted
	 */
	protected static void check(RequiredPerm perm) throws NodeException, InsufficientPrivilegesException {
		// nothing to check
		if (perm.type() == 0) {
			return;
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		if (!t.getPermHandler().checkPermissionBit(perm.type(), perm.id() == 0 ? null : perm.id(), perm.bit())) {
			throw new InsufficientPrivilegesException("Permission Error", "rest.permission.required", (String)null, null, null);
		}
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		try {
			check(resourceInfo.getResourceMethod(), resourceInfo.getResourceClass());
		} catch (InsufficientPrivilegesException e) {
			requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
					.entity(new GenericResponse(
							new Message(Message.Type.CRITICAL, I18NHelper.get("rest.permission.required")),
							new ResponseInfo(ResponseCode.PERMISSION, "Permission Error")))
					.build());
		} catch (NodeException e) {
			NodeLogger.getNodeLogger(getClass()).error(e);
			requestContext.abortWith(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(MiscUtils.serverError()).build());
		}
	}
}
