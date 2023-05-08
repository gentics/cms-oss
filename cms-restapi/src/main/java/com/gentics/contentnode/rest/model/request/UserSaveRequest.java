package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.User;

/**
 * Request used for saving users.
 */
@XmlRootElement
public class UserSaveRequest {

	/**
	 * User which should be saved.
	 */
	private User user;

	public UserSaveRequest() {
	}

	/**
	 * Set the user which should be saved.
	 * 
	 * @param user
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * Return the user which should be saved.
	 * 
	 * @return
	 */
	public User getUser() {
		return user;
	}
}
