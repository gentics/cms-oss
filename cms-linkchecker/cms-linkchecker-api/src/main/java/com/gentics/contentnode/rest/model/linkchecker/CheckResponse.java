package com.gentics.contentnode.rest.model.linkchecker;

import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Response containing the check result
 */
public class CheckResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private Boolean valid;

	private String reason;

	/**
	 * True, when the URL is valid, false if not
	 * @return true for valid
	 */
	public Boolean getValid() {
		return valid;
	}

	/**
	 * Set true for valid URL, false for invalid
	 * @param valid flag
	 * @return fluent API
	 */
	public CheckResponse setValid(Boolean valid) {
		this.valid = valid;
		return this;
	}

	/**
	 * Reason of failure
	 * @return reason
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Set reason
	 * @param reason reason
	 * @return fluent API
	 */
	public CheckResponse setReason(String reason) {
		this.reason = reason;
		return this;
	}
}
