package com.gentics.contentnode.rest.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Container request filter implementation for resources/methods that require a specific feature to be activated
 */
@Provider
@Authenticated
@Priority(Priorities.USER)
public class FeatureRequestFilter implements ContainerRequestFilter {
	@Context
	ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Collection<Feature> features = getFeatures();
		if (features.isEmpty()) {
			return;
		}
		try {
			Trx.operate(() -> {
				for (Feature feature : features) {
					if (!feature.isActivated()) {
						GenericResponse response = new GenericResponse(new Message(Message.Type.CRITICAL,
								I18NHelper.get("rest.feature.required", feature.getName())), new ResponseInfo(ResponseCode.FAILURE, "Feature required"));
						requestContext.abortWith(Response.status(Response.Status.METHOD_NOT_ALLOWED).entity(response).build());
						return;
					}
				}
			});
		} catch (NodeException e) {
			NodeLogger.getNodeLogger(getClass()).error(e);
			requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(MiscUtils.serverError()).build());
		}
	}

	/**
	 * Get the required feature
	 * @return feature
	 */
	protected Collection<Feature> getFeatures() {
		Collection<Feature> features = new ArrayList<>();
		if (resourceInfo.getResourceMethod() != null) {
			for (RequiredFeature required : resourceInfo.getResourceMethod().getAnnotationsByType(RequiredFeature.class)) {
				features.add(required.value());
			}
		}
		if (resourceInfo.getResourceClass() != null) {
			for (RequiredFeature required : resourceInfo.getResourceClass().getAnnotationsByType(RequiredFeature.class)) {
				features.add(required.value());
			}
		}
		return features;
	}
}
