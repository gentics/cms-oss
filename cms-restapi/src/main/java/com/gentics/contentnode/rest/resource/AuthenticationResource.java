package com.gentics.contentnode.rest.resource;

import com.gentics.contentnode.rest.model.request.HashPasswordRequest;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.request.MatchPasswordRequest;
import com.gentics.contentnode.rest.model.request.SetPasswordRequest;
import com.gentics.contentnode.rest.model.response.AuthenticationResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.HashPasswordResponse;
import com.gentics.contentnode.rest.model.response.LoginResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;

/**
 * Authentication Resource. This can be used to authenticate an existing SID.
 */
@Path("/auth")
public interface AuthenticationResource {
	/**
	 * Validate the session
	 *
	 * @return response containing validation result and (possibly) a user
	 */
	@GET
	@Path("/validate")
	AuthenticationResponse validate() throws Exception;

	/**
	 * Perform a login to the system with SSO systems
	 * @return SID or "NOTFOUND" or "FAILURE"
	 */
	@GET
	@Path("/login")
	@Produces("text/plain; charset=UTF-8")
	String alternateSsoLogin();

	/**
	 * Perform a login to the system with SSO systems
	 * @return SID or "NOTFOUND" or "FAILURE"
	 */
	@GET
	@Path("/ssologin")
	@Produces("text/plain; charset=UTF-8")
	String ssoLogin();

	/**
	 * Perform a login to the system based on user credentials.
	 * If the user is successfully authenticated, create a new session and send back the sid.
	 * If a new sessionSecret is created, set it as a cookie
	 * @param request    login request (contains the login credentials)
	 * @return login     response
	 */
	@POST
	@Path("/login")
	LoginResponse login(LoginRequest request) throws Exception;

	/**
	 * Do a logout for the current session
	 * @return generic response
	 */
	@POST
	@Path("/logout")
	GenericResponse logout() throws Exception;

	/**
	 * Create a hash of the given password and userID
	 * The hashing algorithm can change at any time. As this method is possibly
	 * expensive (depends on the implemented hash algorithm), we don't allow
	 * anonymous access to it.
	 * @param  hashPasswordRequest   Password request object
	 * @return HashPasswordResponse
	 */
	@POST
	@Path("/hashpassword")
	HashPasswordResponse hashPassword(
			@Context HttpServletRequest httpServletRequest,
			HashPasswordRequest hashPasswordRequest);

	/**
	 * Checks if the given password matches the given hash
	 * As this method is possibly expensive (depends on the implemented hash algorithm),
	 * we don't allow anonymous access to it.
	 * @param  matchPasswordRequest  Password match request object
	 * @return GenericResponse
	 */
	@POST
	@Path("/matchpassword")
	GenericResponse matchPassword(
			@Context HttpServletRequest httpServletRequest,
			MatchPasswordRequest matchPasswordRequest);

	/**
	 * Returns the global prefix
	 * @return GenericResponse
	 */
	@GET
	@Path("/globalprefix")
	GenericResponse globalPrefix();

	/**
	 * Check whether the token of a password link (see {@code PUT /group/{id}/users?passwordLink=true}) can still be
	 * used. Needs no session. The response does not tell why a token is not valid (expired, used, replaced or unknown).
	 * @param token token
	 * @return response code OK if the token is valid, FAILURE otherwise
	 */
	@GET
	@Path("/password")
	GenericResponse validatePasswordToken(@QueryParam("token") String token) throws Exception;

	/**
	 * Set the password of a user with the token of a password link. Needs no session. On success the token is used up.
	 * @param request token and new password
	 * @return response code OK if the password was set, INVALIDDATA if it does not satisfy the password rule, FAILURE
	 *         if the token is not valid
	 */
	@POST
	@Path("/password")
	GenericResponse setPassword(SetPasswordRequest request) throws Exception;
}
