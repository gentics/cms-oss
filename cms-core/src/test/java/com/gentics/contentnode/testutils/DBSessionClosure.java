package com.gentics.contentnode.testutils;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.supply;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.DBSession;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.object.SystemUser;

/**
 * Session closure
 */
public class DBSessionClosure implements AutoCloseable {
	/**
	 * Previous session (may be null)
	 */
	protected Session previousSession;

	/**
	 * New created session
	 */
	protected DBSession session;

	/**
	 * Create a new session for the given user and set it as current session into {@link ContentNodeHelper}
	 * @param userId user ID
	 * @throws NodeException
	 */
	public DBSessionClosure(int userId) throws NodeException {
		previousSession = ContentNodeHelper.getSession();

		// create a dummy session for the user
		session = supply(t -> {
			SystemUser user = t.getObject(SystemUser.class, userId);
			return new DBSession(user, "localhost", "JUnit Test", DBSession.createSessionSecret(), 0);
		});
		ContentNodeHelper.setSession(session);
	}

	/**
	 * Set the given session as current session into {@link ContentNodeHelper}
	 * @param session session
	 */
	public DBSessionClosure(Session session) {
		previousSession = ContentNodeHelper.getSession();
		ContentNodeHelper.setSession(session);
	}

	/**
	 * Get the session
	 * @return session
	 */
	public Session getSession() {
		return session;
	}

	/**
	 * If a new session was created, do a logout. In any case set the previous session
	 */
	@Override
	public void close() throws NodeException {
		if (session != null) {
			consume(DBSession::logout, session);
		}
		ContentNodeHelper.setSession(previousSession);
	}
}