package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.User;

/**
 * Authentication Result
 * @author norbert
 */
@XmlRootElement
public class AuthenticationResponse extends GenericResponse {

	/**
	 * User
	 */
	private User user;

	/**
	 * Create an instance
	 */
	public AuthenticationResponse() {}

	/**
	 * Get the user
	 * @return user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Set the user
	 * @param user user
	 */
	public void setUser(User user) {
		this.user = user;
	}
}
