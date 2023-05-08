package com.gentics.contentnode.rest.model.linkchecker;

import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Response containing LinkChecker statistics
 */
public class ExternalLinkStatistics extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private int lastRun;

	private int valid;

	private int invalid;

	private int unchecked;

	/**
	 * Timestamp of last successful run of the link checker
	 * @return timestamp
	 */
	public int getLastRun() {
		return lastRun;
	}

	/**
	 * Set timestamp of last run
	 * @param lastRun timestamp
	 * @return fluent API
	 */
	public ExternalLinkStatistics setLastRun(int lastRun) {
		this.lastRun = lastRun;
		return this;
	}

	/**
	 * Number of valid external links
	 * @return valid links
	 */
	public int getValid() {
		return valid;
	}

	/**
	 * Set number of valid links
	 * @param valid number
	 * @return fluent API
	 */
	public ExternalLinkStatistics setValid(int valid) {
		this.valid = valid;
		return this;
	}

	/**
	 * Number of unvalid external links
	 * @return invalid links
	 */
	public int getInvalid() {
		return invalid;
	}

	/**
	 * Set number of invalid links
	 * @param invalid number
	 * @return fluent API
	 */
	public ExternalLinkStatistics setInvalid(int invalid) {
		this.invalid = invalid;
		return this;
	}

	/**
	 * Number of unchecked external links
	 * @return unchecked links
	 */
	public int getUnchecked() {
		return unchecked;
	}

	/**
	 * Set number of unchecked links
	 * @param unchecked number
	 * @return fluent API
	 */
	public ExternalLinkStatistics setUnchecked(int unchecked) {
		this.unchecked = unchecked;
		return this;
	}
}
