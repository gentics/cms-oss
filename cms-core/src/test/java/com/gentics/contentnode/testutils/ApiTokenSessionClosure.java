package com.gentics.contentnode.testutils;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Optional;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.ApiTokenFactory;
import com.gentics.contentnode.auth.ResolvableApiTokenDataModel;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.ApiTokenSession;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.Session;

/**
 * Session closure
 */
public class ApiTokenSessionClosure implements AutoCloseable {
	/**
	 * Previous session (may be null)
	 */
	protected Session previousSession;

	/**
	 * Create a new session for the given API Token and set it as current session into {@link ContentNodeHelper}
	 * @param token API Token
	 * @throws NodeException
	 */
	public ApiTokenSessionClosure(String token) throws NodeException {
		previousSession = ContentNodeHelper.getSession();

		// create a dummy session for the user
		String tokenHash = ApiTokenFactory.hash(token);
		Optional<ResolvableApiTokenDataModel> optToken = supply(() -> ApiTokenFactory.load(tokenHash));
		if (optToken.isPresent()) {
			ApiTokenSession session = supply(() -> new ApiTokenSession(optToken.get()));
			ContentNodeHelper.setSession(session);
		} else {
			throw new InvalidSessionIdException(token);
		}
	}

	/**
	 * If a new session was created, do a logout. In any case set the previous session
	 */
	@Override
	public void close() throws NodeException {
		ContentNodeHelper.setSession(previousSession);
	}
}
