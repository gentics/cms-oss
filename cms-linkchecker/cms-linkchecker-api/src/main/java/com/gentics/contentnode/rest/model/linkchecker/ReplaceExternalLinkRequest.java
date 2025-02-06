package com.gentics.contentnode.rest.model.linkchecker;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to replace an external Link with another URL
 */
@XmlRootElement
public class ReplaceExternalLinkRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 623766549495427686L;

	private String url;

	private ReplaceScope scope = ReplaceScope.link;

	/**
	 * New URL
	 * @documentationExample https://totally.valid.url/
	 * @return new URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the new URL
	 * @param url URL
	 * @return fluent API
	 */
	public ReplaceExternalLinkRequest setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * Replacement scope (default is "link")
	 * @documentationExample all
	 * @return scope
	 */
	public ReplaceScope getScope() {
		return scope;
	}

	/**
	 * Set the scope
	 * @param scope scope
	 * @return fluent API
	 */
	public ReplaceExternalLinkRequest setScope(ReplaceScope scope) {
		this.scope = scope;
		return this;
	}
}
