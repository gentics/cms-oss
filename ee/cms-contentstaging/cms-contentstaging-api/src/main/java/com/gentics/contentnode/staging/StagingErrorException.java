package com.gentics.contentnode.staging;

import com.gentics.api.lib.exception.NodeException;

/**
 * Exception wrapping a {@link StagingError}
 */
public class StagingErrorException extends NodeException {
	private static final long serialVersionUID = -1614035822644080415L;

	/**
	 * Wrapped staging error
	 */
	protected StagingError stagingError;

	/**
	 * Create instance
	 * @param stagingError wrapped error
	 */
	public StagingErrorException(StagingError stagingError) {
		this.stagingError = stagingError;
	}

	/**
	 * Get the wrapped error
	 * @return error
	 */
	public StagingError getStagingError() {
		return stagingError;
	}
}
