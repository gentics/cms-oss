package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.User;

/**
 * Response containing a single user
 */
@XmlRootElement
public class UserLoadResponse extends GenericResponse {

	/**
	 * The loaded user
	 */
	private User user;

	/**
	 * Create an empty instance
	 */
	public UserLoadResponse() {}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public UserLoadResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with message, response info and user
	 * @param message message
	 * @param responseInfo response info
	 * @param user user
	 */
	public UserLoadResponse(Message message, ResponseInfo responseInfo, User user) {
		super(message, responseInfo);
		setUser(user);
	}

	/**
	 * Set the user
	 * @param user user
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * Get the user
	 * @return user
	 */
	public User getUser() {
		return user;
	}
}
