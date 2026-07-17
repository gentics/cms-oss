package com.gentics.contentnode.tests.utils;

import static com.gentics.contentnode.factory.Trx.operate;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.ApiTokenFactory;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.token.ApiTokenCreationRequest;
import com.gentics.contentnode.testutils.ApiTokenSessionClosure;
import com.gentics.contentnode.testutils.DBSessionClosure;

/**
 * Helper for various types of authentication against the REST API
 */
public class Auth {
	/**
	 * User to authenticate
	 */
	protected SystemUser user;

	/**
	 * API Token of the user
	 */
	protected String apiToken;

	/**
	 * Create an instance for the given user. This will create an API Token
	 * @param user user
	 * @throws NodeException
	 */
	public Auth(SystemUser user) throws NodeException {
		this.user = user;
		apiToken = ApiTokenFactory.createToken();
		operate(() -> ApiTokenFactory.create(new ApiTokenCreationRequest().setName("Test Token"), user.getId(), apiToken));
	}

	/**
	 * Execute the given method in the scope of a session of given type
	 * @param <T> type of the response
	 * @param type session type
	 * @param method method to execute
	 * @return response object
	 * @throws NodeException
	 */
	public <T> T withAuth(AuthType type, Supplier<T> method) throws NodeException {
		switch (type) {
		case LOGIN:
			try (DBSessionClosure ses = new DBSessionClosure(user.getId())) {
				return method.supply();
			}
		case TOKEN:
			try (ApiTokenSessionClosure ses = new ApiTokenSessionClosure(apiToken)) {
				return method.supply();
			}
		case NONE:
		default:
			return method.supply();
		}
	}

	/**
	 * Execute the given method in the scope of a session of given type
	 * @param type session type
	 * @param method method to execute
	 * @throws NodeException
	 */
	public void withAuth(AuthType type, Operator method) throws NodeException {
		switch (type) {
		case LOGIN:
			try (DBSessionClosure ses = new DBSessionClosure(user.getId())) {
				method.operate();
			}
			break;
		case TOKEN:
			try (ApiTokenSessionClosure ses = new ApiTokenSessionClosure(apiToken)) {
				method.operate();
			}
			break;
		case NONE:
		default:
			method.operate();
			break;
		}
	}

	/**
	 * Authentication types
	 */
	public static enum AuthType {
		/**
		 * Authenticate by logging in (with username and password)
		 */
		LOGIN,

		/**
		 * Authenticate by sending a valid API Token
		 */
		TOKEN,

		/**
		 * Do not authenticate
		 */
		NONE;
	}
}
