package com.gentics.contentnode.rest.model.linkchecker;

import java.io.Serializable;

/**
 * Request to check the given external link for validity
 */
public class CheckExternalLinkRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private String url;

	/**
	 * URL to check
	 * @return URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the URL to check
	 * @param url to check
	 */
	public void setUrl(String url) {
		this.url = url;
	}
}
