package com.gentics.contentnode.rest.model.response.admin;

import java.io.Serializable;

public class Update implements Serializable {
	private static final long serialVersionUID = 2013503774301132323L;

	protected String version;

	protected String changelogUrl;

	public String getVersion() {
		return version;
	}

	public Update setVersion(String version) {
		this.version = version;
		return this;
	}

	public String getChangelogUrl() {
		return changelogUrl;
	}

	public Update setChangelogUrl(String changelogUrl) {
		this.changelogUrl = changelogUrl;
		return this;
	}
}
