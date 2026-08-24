package com.gentics.contentnode.rest.client;

import java.net.CookieHandler;
import java.util.List;

import org.apache.http.cookie.Cookie;

import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.response.GenericResponse;

import jakarta.ws.rs.client.WebTarget;

/**
 * <p>This client provides wrappers, helper-methods and exception-handling to facilitate requests to the REST API.
 * It is initialized with a URL pointing to the base-location providing the services. After a successful login,
 * a WebTarget-object can be retrieved that is then used to assemble and send requests; response-objects are
 * returned from the server, containing the requested data and further information in the case of an error.</p>
 */
public interface RestApi {

	/**
	 * Logs the specified user into the system using the password given
	 *
	 * @param username user name
	 * @param password password
	 * @throws RestException If the login failed
	 */
	void login(String username, String password) throws RestException;

	/**
	 * Performs login on an SSO system - before this works,
	 * necessary filters have to be defined
	 *
	 * @throws RestException If the login via SSO failed
	 */
	void ssologin() throws RestException;

	/**
	 * Logs out the current user
	 *
	 * @throws RestException If the logout failed
	 */
	void logout() throws RestException;

	/**
	 * Analyzes the response of a finished request and asserts that it was executed without errors;
	 * if a problem occurred during the request, a specialized RestException is thrown
	 *
	 * @param response The response of the request that needs to be checked
	 * @throws RestException Thrown if the request was not successful, and contains further information of the reason of failure
	 */
	void assertResponse(GenericResponse response) throws RestException;

	/**
	 * Checks if the version of the REST API on the server is the same that is used
	 * by the client; if there is a mismatch between the two versions, a RestException is thrown
	 *
	 * @throws RestException Mismatch between the versions detected
	 */
	void assertMatchingVersion() throws RestException;

	/**
	 * Provides access to the WebTarget that is used as the base for all commands to the server
	 *
	 * @return The base resource, with the active SID already set
	 * @throws RestException If no valid SID is registered with the client
	 */
	WebTarget base() throws RestException;

	/**
	 * Set the API Token, that should be used
	 * @param apiToken api token
	 */
	void setApiToken(String apiToken);

	/**
	 * Get the cookies currently stored in the client
	 * @return stored cookies
	 * @throws RestException
	 */
	List<Cookie> getCookies() throws RestException;

	/**
	 * Get the cookie handler
	 * @return cookie handler
	 */
	CookieHandler getCookieHandler();

}