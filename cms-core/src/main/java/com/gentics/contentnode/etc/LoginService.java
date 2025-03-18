package com.gentics.contentnode.etc;

import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This interface defines the methods that a login service must implement for authenticating.
 */
public interface LoginService {

	/**
	 * Configuration item name for cookie's SameSite value
	 */
	String CONFIGURATION_COOKIE_SAMESITE = "session_cookie_samesite";

	/**
	 * Login method to authenticate a user.
	 *
	 * @param username The username of the user.
	 * @param password The password of the user in plain text.
	 * @param factory The content node factory to use.
	 * @param servletRequest The current request.
	 * @param servletResponse The current response.
	 *
	 * @return LoginResponse
	 */
	LoginResponse login(String username, String password, ContentNodeFactory factory, HttpServletRequest servletRequest, HttpServletResponse servletResponse);

	/**
	 * Whether this service is the default login service.
	 *
	 * <p>
	 *     If {@code true} the response of this service should be used for failed login attempts.
	 * </p>
	 * @return Whether this service is the default login service.
	 */
	default boolean isDefaultService() {
		return false;
	}

	/**
	 * Determine whether the secure flag of the session cookie should be set.
	 * @return true for secure, false otherwise
	 */
	static boolean isCookieSecure() throws TransactionException {
		return NodeConfigRuntimeConfiguration.getPreferences().getFeature("secure_cookie");
	}

}
