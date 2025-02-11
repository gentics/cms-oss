package com.gentics.contentnode.etc;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.*;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.lib.http.CookieHelper;
import com.gentics.lib.log.NodeLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractLoginService implements LoginService {

	protected final NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Does the login with the given username and password, while the password can be null to skip the password check.
	 *
	 * @param username User name
	 * @param password The password or null to not check it
	 * @param response The response object, will be used to set a proper response info message
	 * @param checkPassword Whether to check the given password for match or not
	 *
	 * @return A valid user object or null
	 *
	 * @throws NodeException
	 */
	public SystemUser performLogin(String username, String password, LoginResponse response, boolean checkPassword, ContentNodeFactory factory) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Get the user from the given credentials
		SystemUser systemUser = ((SystemUserFactory) t.getObjectFactory(SystemUser.class))
			.getSystemUser(username, password, checkPassword);

		// Credentials must match and password not be empty
		// Prohibit login for users that are not member of a single group
		if (systemUser == null || (password != null && password.isEmpty()) || systemUser.getUserGroups().isEmpty()) {
			response.setResponseInfo(new ResponseInfo(ResponseCode.NOTFOUND, "Did not find a user with given credentials"));

			if (checkPassword) {
				// log failed login attempt
				ActionLogger.securityLogger.error(String.format("(%s) Login failed", username));
				ActionLogger.logCmd(
					ActionLogger.LOGIN_FAILED,
					SystemUser.TYPE_SYSTEMUSER,
					-1,
					t.getUnixTimestamp(),
					String.format("restApi:auth/login failed for username %s", username));
			}

			return null;
		}

		// We need to create a new session otherwise the permhandler will not be set correctly.
		Transaction userTransaction = factory.startTransaction(null, systemUser.getId(), true);

		try {
			// Check whether the maintenance mode is enabled and the tuser has the permissions to view the maintenance setting.
			// We allow login only when the user has the permission to view and therefore change the maintenance setting.
			if (MaintenanceMode.get().isEnabled()) {
				PermHandler permHandler = userTransaction.getPermHandler();

				// There is only one entry for system maintenance, so there is no need to provide an object id.
				if (!permHandler.checkPermissionBit(PermHandler.TYPE_MAINTENCANCE, null, PermHandler.PERM_VIEW)) {
					response.setResponseInfo(
						new ResponseInfo(ResponseCode.MAINTENANCEMODE, "The maintenance mode is currently enabled. Login was therefore disabled."));

					return null;
				}
			}
		} finally {
			userTransaction.commit();
			TransactionManager.setCurrentTransaction(t);
		}

		ActionLogger.logCmd(ActionLogger.LOGIN, SystemUser.TYPE_SYSTEMUSER, systemUser.getId(), t.getUnixTimestamp(), "restApi:auth/login");

		return systemUser;
	}


	/**
	 * Creates a new user session and sets the SESSION_SECRET cookie
	 * if possible
	 * @param systemUser The user for who to create an authenticated session
	 * @return A session token (if cookie is not set it contains the secret also)
	 */
	public String createUserSession(SystemUser systemUser, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws NodeException {
		// Create a new session for the user
		Session session = new Session(
			systemUser, servletRequest != null ? servletRequest.getRemoteAddr() : "",
			servletRequest != null ? servletRequest.getHeader("user-agent") : "",
			null,
			0);

		if (servletResponse != null) {
			NodePreferences prefs = TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences();
			String sameSiteString = ObjectTransformer.getString(prefs.getProperty(CONFIGURATION_COOKIE_SAMESITE), null);

			// Set the session secret as cookie
			CookieHelper.setCookie(SessionToken.SESSION_SECRET_COOKIE_NAME,
				session.getSessionSecret(),
				"/",
				null,
				LoginService.isCookieSecure(),
				true,
				CookieHelper.SameSite.parse(sameSiteString),
				servletResponse);

			return Integer.toString(session.getSessionId());
		}

		return session.getSessionId() + session.getSessionSecret();
	}
}
