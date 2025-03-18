package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.response.LoginResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.ModelBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Login service which authenticates user via given username and password.
 */
public class UsernamePasswordLoginService extends AbstractLoginService {

	@Override
	public LoginResponse login(String username, String password, ContentNodeFactory factory, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		var response = new LoginResponse();

		try {
			SystemUser systemUser = performLogin(username, password, response, true, factory);

			if (systemUser == null) {
				if (response.getResponseInfo() == null) {
					response.setResponseInfo(new ResponseInfo(ResponseCode.NOTFOUND, "Did not find a user with the given credentials"));
				}
			} else {
				response.setSid(createUserSession(systemUser, servletRequest, servletResponse));
				response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully performed login"));
				response.setUser(ModelBuilder.getUser(systemUser));

				TransactionManager.getCurrentTransaction().commit(false);
			}
		} catch (NodeException e) {
			logger.error("Error while logging in user {" + username + "}", e);
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		}

		return response;
	}

	@Override
	public boolean isDefaultService() {
		return true;
	}
}
