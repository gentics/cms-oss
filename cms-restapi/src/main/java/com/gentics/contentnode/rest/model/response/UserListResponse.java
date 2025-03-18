package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.User;

/**
 * Response containing a list of users
 */
@XmlRootElement
public class UserListResponse extends GenericResponse {

	/**
	 * List of Users
	 */
	private List<User> users;

	/**
	 * Create an empty instance
	 */
	public UserListResponse() {}

	/**
	 * Create instance with message and responseinfo
	 * @param message message
	 * @param responseInfo responseinfo
	 */
	public UserListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Get the users
	 * @return users
	 */
	public List<User> getUsers() {
		return users;
	}

	/**
	 * Set the users
	 * @param users users
	 */
	public void setUsers(List<User> users) {
		this.users = users;
	}
}
