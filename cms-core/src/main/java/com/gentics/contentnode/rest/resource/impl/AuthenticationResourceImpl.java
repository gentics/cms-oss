package com.gentics.contentnode.rest.resource.impl;

import java.util.Map;
import java.util.Optional;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.LoginService;
import com.gentics.contentnode.etc.MaintenanceMode;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.NodeSetup;
import com.gentics.contentnode.etc.NodeSetupValuePair;
import com.gentics.contentnode.factory.DBSession;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.request.HashPasswordRequest;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.request.MatchPasswordRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.HashPasswordResponse;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.AuthenticationResource;
import com.gentics.contentnode.security.AccessControlService;
import com.gentics.lib.http.CookieHelper;
import com.gentics.lib.http.CookieHelper.SameSite;
import com.gentics.lib.log.NodeLogger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

/**
 * Authentication Resource. This can be used to authenticate an existing SID.
 *
 * @author norbert
 */
@Produces({ MediaType.APPLICATION_JSON })
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
	 * Session Secret from the Session Secret Cookie
	 */
	private String sessionSecret;

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

	@Override
	@GET
	@Path("/ssologin")
	@Produces("text/plain; charset=UTF-8")
	public String ssoLogin() {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			SessionToken sessionToken = null;
			// the request may not always be set (e.g. when testing)
			if (getRequest() != null) {
				sessionToken = new SessionToken(getRequest());
			} else {
				sessionToken = new SessionToken(getSessionSecret());
			}
			Optional<DBSession> optSession = DBSession.load(sessionToken);

			if (optSession.isPresent()) {

				// We need to create a new session otherwise the permhandler will not be set correctly.
				Transaction userTransaction = getFactory().startTransaction(null, optSession.get().getUserId(), true);

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
					CookieHelper.setCookie(SessionToken.SESSION_SECRET_COOKIE_NAME, optSession.get().getCookieValue(), "/", null, LoginService.isCookieSecure(), true, SameSite.parse(sameSiteString), getResponse());
				}
				trx.success();
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

	@Override
	@POST
	@Path("/login")
	public LoginResponse login(LoginRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			String username = request.getLogin();
			String password = request.getPassword();

			LoginResponse response = tryLoginWithService(username, password);

			if (response != null) {
				return response;
			}

			var message = "No login service returned a response";

			logger.error("Error while logging in user {" + username + "}: " + message);

			response = new LoginResponse();
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, message));

			trx.success();
			return response;
		}
	}

	/**
	 * Search a login implementation that is able to perform a login
	 * @param username user login
	 * @param password user password
	 * @return Login response if login was successfully
	 */
	private LoginResponse tryLoginWithService(String username, String password) {
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

	@Override
	@POST
	@Path("/logout/{sid}")
	public GenericResponse logout() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			SessionToken sessionToken = new SessionToken(getSessionSecret());
			Optional<DBSession> optSession = DBSession.load(sessionToken);

			if (optSession.isPresent()) {
				optSession.get().logout();
				CookieHelper.setCookie(SessionToken.SESSION_SECRET_COOKIE_NAME,
						"deleted", "/", 0, LoginService.isCookieSecure(), true, null, getResponse());

				trx.success();
				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully logged out"));
			} else {
				return new GenericResponse(null, new ResponseInfo(ResponseCode.INVALIDDATA, "Invalid SID given"));
			}
		}
	}

	@Override
	@POST
	@Path("/hashpassword")
	public HashPasswordResponse hashPassword(
			@Context HttpServletRequest httpServletRequest,
			HashPasswordRequest hashPasswordRequest) {
		HashPasswordResponse response = new HashPasswordResponse();

		String password = hashPasswordRequest.getPassword();
		int userId      = hashPasswordRequest.getUserId();

		try {
			if (!isAuthenticatedOrIpWhiteListed(httpServletRequest)) {
				throw new InvalidSessionIdException("");
			}

			// Hash the plain text password
			String hashedPassword = SystemUserFactory.hashPassword(
					password, userId);

			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully hashed password"));
			response.setHash(hashedPassword);
		} catch (InvalidSessionIdException e) {
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} catch (NodeException e) {
			logger.error("Error while hashing password for user ID {" + userId + "}", e);
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, "Error while hashing password"));
		}

		return response;
	}

	@Override
	@POST
	@Path("/matchpassword")
	public GenericResponse matchPassword(
			@Context HttpServletRequest httpServletRequest,
			MatchPasswordRequest matchPasswordRequest) {
		GenericResponse response = new GenericResponse();

		try {
			if (!isAuthenticatedOrIpWhiteListed(httpServletRequest)) {
				throw new InvalidSessionIdException("");
			}

			if (SystemUserFactory.passwordMatches(
					matchPasswordRequest.getPassword(), matchPasswordRequest.getHash())) {
				response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Password matches"));
			} else {
				response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, "Password does not match"));
			}

		} catch (InvalidSessionIdException e) {
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} catch (NodeException e) {
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
	 * @return True if granted
	 * @throws NodeException
	 */
	protected final boolean isAuthenticatedOrIpWhiteListed(HttpServletRequest httpServletRequest)
			throws NodeException {
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
			if (!accessVerified) {
				SessionToken sessionToken = new SessionToken(getSessionSecret());
				Optional<DBSession> optSession = DBSession.load(sessionToken);

				if (optSession.isPresent()) {
					accessVerified = true;
				}
			}
		} catch (InvalidSessionIdException e) {
			return false;
		}

		return accessVerified;
	}

}

