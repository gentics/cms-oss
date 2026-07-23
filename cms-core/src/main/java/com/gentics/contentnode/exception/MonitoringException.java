package com.gentics.contentnode.exception;

import com.gentics.contentnode.rest.model.response.ResponseCode;
import jakarta.ws.rs.core.Response.Status;

/**
 * Exception for failed health checks.
 */
public class MonitoringException extends RestMappedException {

	/**
	 * Default constructor.
	 *
	 * @param reason The failure reason.
	 * @param status The status code for the response.
	 */
	public MonitoringException(String reason, Status status) {
		super(reason);

		setResponseCode(ResponseCode.FAILURE);
		setStatus(status);
	}
}
