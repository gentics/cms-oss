package com.gentics.contentnode.rest.model.linkchecker;

/**
 * REST Model of check stati values for external Links
 */
public enum ExternalLinkStatus {
	/**
	 * Link is invalid
	 */
	invalid(-1),

	/**
	 * Link is unchecked
	 */
	unchecked(0),

	/**
	 * Link is valid
	 */
	valid(1);

	/**
	 * Code stored in DB
	 */
	private int code;

	/**
	 * Create instance
	 * @param code code
	 */
	private ExternalLinkStatus(int code) {
		this.code = code;
	}

	/**
	 * Get DB code
	 * @return code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get the status for the given code.
	 * @param code code
	 * @return status (returns unchecked, if code is not found)
	 */
	public static ExternalLinkStatus getForCode(int code) {
		for (ExternalLinkStatus status : values()) {
			if (status.code == code) {
				return status;
			}
		}

		return unchecked;
	}
}
