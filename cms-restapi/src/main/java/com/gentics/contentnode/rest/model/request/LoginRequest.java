package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Login Request containing the user credentials
 */
@XmlRootElement
public class LoginRequest {

	/**
	 * Login name
	 */
	protected String login;

	/**
	 * Password
	 */
	protected String password;

	/**
	 * Create an empty instance
	 */
	public LoginRequest() {}

	/**
	 * Get the login name
	 * @return login name
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * Set the login name
	 * @param login login name
	 */
	public void setLogin(String login) {
		this.login = login;
	}

	/**
	 * Get the password
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password
	 * @param password password
	 */
	public void setPassword(String password) {
		this.password = password;
	}
}
