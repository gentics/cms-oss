package com.gentics.contentnode.rest.model.response.admin;

import java.io.Serializable;

/**
 * An available update
 */
public class Update implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2013503774301132323L;

	/**
	 * Version
	 */
	protected String version;

	/**
	 * Changelog URL
	 */
	protected String changelogUrl;

	/**
	 * Version of the Update
	 * @return version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the version
	 * @param version version
	 * @return fluent API
	 */
	public Update setVersion(String version) {
		this.version = version;
		return this;
	}

	/**
	 * Changelog URL
	 * @return changelog URL
	 */
	public String getChangelogUrl() {
		return changelogUrl;
	}

	/**
	 * Set the changelog URL
	 * @param changelogUrl URL
	 * @return fluent API
	 */
	public Update setChangelogUrl(String changelogUrl) {
		this.changelogUrl = changelogUrl;
		return this;
	}
}
