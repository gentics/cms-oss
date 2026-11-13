package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to set the password of a user with the token of a password link
 */
@XmlRootElement
public class SetPasswordRequest {
	/**
	 * Token from the password link
	 */
	private String token;

	/**
	 * New plain text password
	 */
	private String password;

	/**
	 * Constructor for JAXB
	 */
	public SetPasswordRequest() {}

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
	public SetPasswordRequest setToken(String token) {
		this.token = token;
		return this;
	}

	/**
	 * New plain text password: 4 to 20 characters, only letters, digits and the characters _ - . @
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password
	 * @param password password
	 * @return fluent API
	 */
	public SetPasswordRequest setPassword(String password) {
		this.password = password;
		return this;
	}
}
