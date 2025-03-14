package com.gentics.contentnode.rest.resource.impl;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.LoginService;
import com.gentics.contentnode.etc.MaintenanceMode;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.NodeSetup;
import com.gentics.contentnode.etc.NodeSetupValuePair;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.request.HashPasswordRequest;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.request.MatchPasswordRequest;
import com.gentics.contentnode.rest.model.response.AuthenticationResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.HashPasswordResponse;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.AuthenticationResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.security.AccessControlService;
import com.gentics.lib.http.CookieHelper;
import com.gentics.lib.http.CookieHelper.SameSite;
import com.gentics.lib.log.NodeLogger;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import java.util.Map;

/**
 * Authentication Resource. This can be used to authenticate an existing SID.
 *
 * @author norbert
 */
@Path("/auth")
public class AuthenticationResourceImpl extends AbstractLoginResource implements AuthenticationResource {

	/**
	 * Logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Access control
	 */
	protected AccessControlService accessControlService = new AccessControlService("restapi");


	/**
	 * Session id that is injected by JAX-RS.
	 */
	private String sessionId;

	/**
	 * Session Secret from the Session Secret Cookie
	 */
	private String sessionSecret;



	/**
	 * Initializes this ResourceImpl
	 */
	@Override
	@PostConstruct
	public void initialize() {

		super.initialize();
	}

	/**
	 * Set the sessionId for this request.
	 * 
	 * @param sessionId Id of the session to use for this ContentNodeResource
	 */
	@QueryParam("sid")
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Get the session Id of the current request
	 * @return The session Id of the current request
	 */
	public String getSessionId() {
		return sessionId;
	}

	@Context
	public void setSessionSecretFromCookie(HttpHeaders headers) {
		Map<String, Cookie> cookies = headers.getCookies();

		// cookies may be null
		if (null == cookies) {
			return;
		}
		Cookie sessionSecretCookie = cookies.get(SessionToken.SESSION_SECRET_COOKIE_NAME);

		if (null == sessionSecretCookie) {
			return;
		}
		this.sessionSecret = sessionSecretCookie.getValue();
	}

	public void setSessionSecret(String sessionSecret) {
		this.sessionSecret = sessionSecret;
	}

	public String getSessionSecret() {
		return sessionSecret;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.AuthenticationResource#validate(java.lang.String)
	 */
	@Override
	@GET
	@Path("/validate/{sid}")
	public AuthenticationResponse validate(@PathParam("sid") String sid) {
		AuthenticationResponse response = new AuthenticationResponse();

		// check the SID
		try {
			int userId = validateSID(sid);

			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully validated given SID"));
			response.setUser(ModelBuilder.getUser(transaction.getObject(SystemUser.class, userId)));
		} catch (Exception e) {
			response.setResponseInfo(new ResponseInfo(ResponseCode.INVALIDDATA, "Invalid SID given"));
		}
		return response;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.AuthenticationResource#ssoLogin()
	 */
	@Override
	@GET
	@Path("/ssologin")
	@Produces("text/plain; charset=UTF-8")
	public String ssoLogin() {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			SessionToken sessionToken = null;
			// the request may not always be set (e.g. when testing)
			if (getRequest() != null) {
				sessionToken = new SessionToken(getRequest());
			} else {
				sessionToken = new SessionToken(getSessionId(), getSessionSecret());
			}
			Session session = new Session(sessionToken.getSessionId(), t);

			if (sessionToken.authenticates(session)) {

				// We need to create a new session otherwise the permhandler will not be set correctly.
				Transaction userTransaction = getFactory().startTransaction(null, session.getUserId(), true);

				try {
					// Check whether the maintenance mode is enabled and the the user has the permissions to view the maintenance setting.
					// We allow login only when the user has the permission to view and therefore change the maintenance setting.
					if (MaintenanceMode.get().isEnabled()) {
						PermHandler permHandler = userTransaction.getPermHandler();
						final int MAINTENCANCE_TREE_NODE_ID = 84;

						if (!permHandler.checkPermissionBit(PermHandler.TYPE_MAINTENCANCE, MAINTENCANCE_TREE_NODE_ID, PermHandler.PERM_VIEW)) {
							logger.info("The maintenance mode is currently enabled. Login was therefore disabled.");
							return ResponseCode.MAINTENANCEMODE.toString();
						}
					}
				} finally {
					userTransaction.commit();
					TransactionManager.setCurrentTransaction(t);
				}

				// set the session secret as cookie
				if (getResponse() != null) {
					NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
					String sameSiteString = ObjectTransformer.getString(prefs.getProperty(LoginService.CONFIGURATION_COOKIE_SAMESITE), null);
					CookieHelper.setCookie(SessionToken.SESSION_SECRET_COOKIE_NAME, session.getSessionSecret(), "/", null, LoginService.isCookieSecure(), true, SameSite.parse(sameSiteString), getResponse());
				}
				// return the sid as plaintext
				return ObjectTransformer.getString(sessionToken.getSessionId(), "");
			} else {
				throw new InvalidSessionIdException(ObjectTransformer.getString(sessionToken.getSessionId(), ""));
			}
		} catch (InvalidSessionIdException e) {
			return ResponseCode.NOTFOUND.toString();
		} catch (Exception e) {
			return ResponseCode.FAILURE.toString();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.AuthenticationResource#alternateSsoLogin()
	 */
	@Override
	@GET
	@Path("/login")
	@Produces("text/plain; charset=UTF-8")
	public String alternateSsoLogin() {
		return ssoLogin();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.AuthenticationResource#login(com.gentics.contentnode.rest.model.request.LoginRequest)
	 */
	@Override
	@POST
	@Path("/login")
	public LoginResponse login(LoginRequest request, @QueryParam("sid") @DefaultValue("0") String sidString) {
		String username = request.getLogin();
		String password = request.getPassword();

		LoginResponse response = tryLoginWithService(username, password, sidString);

		if (response != null) {
			return response;
		}

		var message = "No login service returned a response";

		logger.error("Error while logging in user {" + username + "}: " + message);

		response = new LoginResponse();
		response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, message));

		return response;
	}

	/**
	 * Search a login implementation that is able to perform a login
	 * @param username user login
	 * @param password user password
	 * @param sid session id
	 * @return Login response if login was successfully
	 */
	private LoginResponse tryLoginWithService(String username, String password, String sid) {
		LoginResponse errorResponse = null;

		for (LoginService service : LOGIN_SERVICE_LOADER) {
			LoginResponse loginResponse = service.login(username, password, getFactory(), getRequest(), getResponse());

			if (loginResponse != null) {
				if (loginResponse.getResponseInfo().getResponseCode() == ResponseCode.OK) {
					return loginResponse;
				} else if (errorResponse == null || service.isDefaultService()) {
					errorResponse = loginResponse;
				}
			}
		}

		return errorResponse;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.AuthenticationResource#logout(java.lang.String)
	 */
	@Override
	@POST
	@Path("/logout/{sid}")
	public GenericResponse logout(@PathParam("sid") String sid,
			@QueryParam("allSessions") @DefaultValue("false") boolean allSessions) {
		try {
			SessionToken sessionToken = new SessionToken(sid, getSessionSecret());
			Session session = new Session(sessionToken.getSessionId(), transaction);

			if (sessionToken.authenticates(session)) {
				if (allSessions) {
					session.logoutAllSessions();
					// Remove the session secret cookie
					CookieHelper.setCookie(SessionToken.SESSION_SECRET_COOKIE_NAME,
							"deleted", "/", 0, LoginService.isCookieSecure(), true, null, getResponse());
				} else {
					// Simply log out the current session
					session.logout();
				}

				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully logged out"));
			} else {
				return new GenericResponse(null, new ResponseInfo(ResponseCode.INVALIDDATA, "Invalid SID given"));
			}
		} catch (Exception e) {
			return new GenericResponse(null, new ResponseInfo(ResponseCode.FAILURE, "Error while logout"));
		}
	}



	/**
	 * Validate the given SID
	 * @param sid sid to validate
	 * @return user id
	 * @throws Exception if the SID cannot be validated
	 */
	protected int validateSID(String sid) throws Exception {
		SessionToken sessionToken = new SessionToken(sid);
		Session session = new Session(sessionToken.getSessionId(), transaction);

		int userId = session.getUserId();
		if (userId > 0 && sessionToken.authenticates(session)) {
			return userId;
		} else {
			throw new NodeException("SessionToken does not authenticate the session");
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.AuthenticationResource#hashPassword(
	 *            com.gentics.contentnode.rest.model.request.HashPasswordRequest)
	 */
	@Override
	@POST
	@Path("/hashpassword")
	public HashPasswordResponse hashPassword(
			@Context HttpServletRequest httpServletRequest,
			HashPasswordRequest hashPasswordRequest,
			@QueryParam("sid") @DefaultValue("0") int sessionId) {
		HashPasswordResponse response = new HashPasswordResponse();

		String password = hashPasswordRequest.getPassword();
		int userId      = hashPasswordRequest.getUserId();

		try {
			if (!isAuthenticatedOrIpWhiteListed(httpServletRequest, sessionId)) {
				throw new InvalidSessionIdException(Integer.toString(sessionId));
			}

			// Hash the plain text password
			String hashedPassword = SystemUserFactory.hashPassword(
					password, userId);

			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully hashed password"));
			response.setHash(hashedPassword);
		} catch (InvalidSessionIdException e) {
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} catch (TransactionException e) {
			logger.error("Error while hashing password for user ID {" + userId + "}", e);
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, "Error while hashing password"));
		}

		return response;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.AuthenticationResource#matchPassword(
	 *            com.gentics.contentnode.rest.model.request.MatchPasswordRequest)
	 */
	@Override
	@POST
	@Path("/matchpassword")
	public GenericResponse matchPassword(
			@Context HttpServletRequest httpServletRequest,
			MatchPasswordRequest matchPasswordRequest,
			@QueryParam("sid") @DefaultValue("0") int sessionId) {
		GenericResponse response = new GenericResponse();

		try {
			if (!isAuthenticatedOrIpWhiteListed(httpServletRequest, sessionId)) {
				throw new InvalidSessionIdException(Integer.toString(sessionId));
			}

			if (SystemUserFactory.passwordMatches(
					matchPasswordRequest.getPassword(), matchPasswordRequest.getHash())) {
				response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Password matches"));
			} else {
				response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, "Password does not match"));
			}

		} catch (InvalidSessionIdException e) {
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} catch (TransactionException e) {
			logger.error("Error while hashing password", e);
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, "Error while hashing password"));
		}

		return response;
	}

	/**
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.AuthenticationResource#globalPrefix()
	 */
	@Override
	@GET
	@Path("/globalprefix")
	public GenericResponse globalPrefix() {
		GenericResponse genericResponse = new GenericResponse();

		try {
			// Those are the first 4 license key digits from the license key.
			NodeSetupValuePair globalPrefixValuePair = NodeSetup.getKeyValue(NodeSetup.NODESETUP_KEY.globalprefix);
			genericResponse.setResponseInfo(new ResponseInfo(
					ResponseCode.OK, globalPrefixValuePair.getTextValue()));
		} catch (NodeException e) {
			genericResponse.setResponseInfo(new ResponseInfo(
					ResponseCode.FAILURE, "Error while getting the global prefix: "));
		}

		return genericResponse;
	}

	/**
	 * Checks if the session is authenticated or the IP from the HttpServletRequest
	 * is whitelisted.
	 * @param httpServletRequest The HTTP servlet request from jersey
	 * @param sessionId          Session ID
	 * @return True if granted
	 * @throws TransactionException -
	 */
	protected final boolean isAuthenticatedOrIpWhiteListed(HttpServletRequest httpServletRequest, int sessionId)
			throws TransactionException {
		// Access to this method will only be granted if the IP
		// is whitelisted or a valid session exists.
		boolean accessVerified = false;

		try {
			// Allow if rest method is not called over HTTP
			if (httpServletRequest == null) {
				accessVerified = true;
			}

			// Allow if IP is whitelisted
			if (!accessVerified) {
				accessVerified = accessControlService.verifyAccess(httpServletRequest, null);
			}

			// Allow if authentication is authenticated
			if (!accessVerified && sessionId > 0) {
				SessionToken sessionToken = new SessionToken(sessionId, getSessionSecret());
				Session session = new Session(sessionToken.getSessionId(), transaction);

				if (sessionToken.authenticates(session)) {
					accessVerified = true;
				}
			}
		} catch (InvalidSessionIdException e) {
			return false;
		}

		return accessVerified;
	}

}

