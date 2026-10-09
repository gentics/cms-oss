package com.gentics.contentnode.mcp.util;

/**
 * Thrown when the enterprise search (module or {@code elasticsearch} feature) is not available. Tool calls failing
 * with it are answered with the problem type {@code search-unavailable}.
 */
public class SearchUnavailableException extends RuntimeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4302754466018521431L;

	/**
	 * Create an instance
	 * @param message message
	 * @param cause cause, may be null
	 */
	public SearchUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
