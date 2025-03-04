package com.gentics.contentnode.rest.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;

import com.gentics.contentnode.rest.model.request.HashPasswordRequest;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.request.LoginWithRsaRequest;
import com.gentics.contentnode.rest.model.request.MatchPasswordRequest;
import com.gentics.contentnode.rest.model.response.AuthenticationResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.HashPasswordResponse;
import com.gentics.contentnode.rest.model.response.LoginResponse;

/**
 * Authentication Resource. This can be used to authenticate an existing SID.
 */
@Path("/auth")
public interface AuthenticationResource {

	/**
	 * Validate the given SID
	 *
	 * @param sid
	 *            sid + gcn_session_secret (taken from the GCN_SESSION_SECRET cookie) to validate
	 * @return response containing validation result and (possibly) a user
	 */
	@GET
	@Path("/validate/{sid}")
	AuthenticationResponse validate(@PathParam("sid") String sid);

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
	 * @param sidString  Optional: Existing sid number, the stored secret must match the cookie
	 * @return login     response
	 */
	@POST
	@Path("/login")
	LoginResponse login(LoginRequest request, @QueryParam("sid") @DefaultValue("0") String sidString);
	

	/**
	 * Do a logout for the current session
	 * @return generic response
	 */
	@POST
	@Path("/logout/{sid}")
	GenericResponse logout(@PathParam("sid") String sid,
			@QueryParam("allSessions") @DefaultValue("0") boolean allSessions);

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
			HashPasswordRequest hashPasswordRequest,
			@QueryParam("sid") @DefaultValue("0") int sessionId);

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
			MatchPasswordRequest matchPasswordRequest,
			@QueryParam("sid") @DefaultValue("0") int sessionId);

	/**
	 * Returns the global prefix
	 * @return GenericResponse
	 */
	@GET
	@Path("/globalprefix")
	GenericResponse globalPrefix();
}
