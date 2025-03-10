package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response to a login request
 */
@XmlRootElement
public class LoginResponse extends AuthenticationResponse {

	/**
	 * Session ID
	 */
	private String sid;

	/**
	 * Create an instance
	 */
	public LoginResponse() {}

	/**
	 * Get the session id
	 * @return session id
	 */
	public String getSid() {
		return sid;
	}

	/**
	 * Set the session id
	 * @param sid session id
	 */
	public void setSid(String sid) {
		this.sid = sid;
	}
}
