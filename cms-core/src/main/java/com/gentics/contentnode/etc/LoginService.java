package com.gentics.contentnode.etc;

import com.gentics.contentnode.rest.model.response.LoginResponse;

/**
 * This interface defines the methods that a login service must implement for authenticating.
 */
public interface LoginService {

	/**
	 * Login method to authenticate a user.
	 * @param username The username of the user.
	 * @param password The password of the user in plain text.
	 * @param sid The session ID
	 * @return LoginResponse
	 */
	LoginResponse login(String username, String password, String sid);

}
