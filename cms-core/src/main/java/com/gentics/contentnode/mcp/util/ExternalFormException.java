package com.gentics.contentnode.mcp.util;

/**
 * Thrown when a tool would write to an external form, which comes from its external system. Tools failing with it are
 * answered with the problem type {@code external-form-read-only}.
 */
public class ExternalFormException extends RuntimeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6092147741905583121L;

	/**
	 * Create an instance
	 * @param message message
	 */
	public ExternalFormException(String message) {
		super(message);
	}
}
