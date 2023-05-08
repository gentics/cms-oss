package com.gentics.contentnode.distributed;

import java.io.Serializable;

import javax.ws.rs.WebApplicationException;

import com.gentics.api.lib.exception.NodeException;

/**
 * {@link Serializable} replacement for {@link WebApplicationException}, that
 * can be thrown by implementations of {@link TrxCallable}, which are run
 * in distributed environments.
 */
public class DistributedWebApplicationException extends NodeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -9112828376883276556L;

	protected String message;

	protected int status;

	/**
	 * Empty constructor
	 */
	public DistributedWebApplicationException() {
	}

	/**
	 * Create instance with message and status
	 * @param message message
	 * @param status status
	 */
	public DistributedWebApplicationException(String message, int status) {
		this.message = message;
		this.status = status;
	}

	/**
	 * Transform to an instance of {@link WebApplicationException}
	 * @return exception
	 */
	public WebApplicationException transform() {
		return new WebApplicationException(message, status);
	}
}
