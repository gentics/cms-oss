package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to check the token of a password link. The token is sent in the body, because a query parameter would end up
 * in the access logs of the CMS and of any proxy in front of it.
 */
@XmlRootElement
public class PasswordTokenRequest {
	/**
	 * Token from the password link
	 */
	private String token;

	/**
	 * Constructor for JAXB
	 */
	public PasswordTokenRequest() {}

	/**
	 * Token from the password link
	 * @return token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Set the token
	 * @param token token
	 * @return fluent API
	 */
	public PasswordTokenRequest setToken(String token) {
		this.token = token;
		return this;
	}
}
