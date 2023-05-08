package com.gentics.contentnode.exception;

import javax.ws.rs.core.Response.Status;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;

/**
 * Exception that is thrown when a required feature is not activated
 */
public class FeatureRequiredException extends RestMappedException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8438878103056410223L;

	/**
	 * Create instance for the feature
	 * @param feature feature
	 */
	public FeatureRequiredException(Feature feature) {
		super(I18NHelper.get("rest.feature.required", feature.getName()));
		setMessageType(Message.Type.CRITICAL);
		setResponseCode(ResponseCode.FAILURE);
		setStatus(Status.METHOD_NOT_ALLOWED);
	}
}
