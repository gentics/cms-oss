package com.gentics.contentnode.rest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import com.gentics.contentnode.rest.resource.impl.AbstractContentNodeResource;

/**
 * This ContainerResponseFilter is a hack to give AbstractContentNodeResource
 * a chance to commit the transaction before the Response is committed. This
 * way, an exception during the commit can still produce a HTTP error response.
 * Previously, this was handled by a method in AbstractContentNodeResource that
 * had a \@PreDestroy-Annotation. But when the method was called, the response
 * was already committed and setting an error message would be ignored.
 *
 * @author escitalopram
 *
 */
public class CommittingResponseFilter implements ContainerResponseFilter {

	/**
	 * This method has AbstractContentNodeResource commit its transaction,
	 * if there is an AbstractContentNodeResource object associated with
	 * the request.
	 */
	public void filter(ContainerRequestContext request, ContainerResponseContext response) {
		Object acnrObject = request.getProperty(AbstractContentNodeResource.ACNR_SELF);
		if (acnrObject instanceof AbstractContentNodeResource) {
			((AbstractContentNodeResource) acnrObject).commitTransaction();
		}
	}

}
