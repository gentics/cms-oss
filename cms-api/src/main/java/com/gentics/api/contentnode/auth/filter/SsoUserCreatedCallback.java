package com.gentics.api.contentnode.auth.filter;

import java.util.Map;

import com.gentics.contentnode.rest.model.User;

/**
 * Callback to be executed when a new SSO user has been created.
 */
public interface SsoUserCreatedCallback {
	/**
	 * This method will be called when the new user has been created.
	 * 
	 * @param user The newly created user
	 * @param attributes The attributes provided by the SSO mechanism
	 */
	void accept(User user, Map<String, Object> attributes);
}
