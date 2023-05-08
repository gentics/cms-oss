/*
 * @author tobiassteiner
 * @date Feb 6, 2011
 * @version $Id: Session.java,v 1.1.2.2 2011-02-26 08:51:52 tobiassteiner Exp $
 */
package com.gentics.contentnode.factory;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.Language;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.scheduler.SimpleScheduler;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.i18n.LanguageProvider;
import com.gentics.lib.log.NodeLogger;

public class Session implements LanguageProvider, Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4588712889085187996L;

	private static ScheduledFuture sessionCleaningFuture;

	protected final int sessionId;
	protected final int userId;
	protected final int languageId;
	protected final Language language;
	protected final String sessionSecret;

	/**
	 * Initializes a new instance from a database connection.
	 *
	 * @param sessionId the primary key into the systemsession table.
	 * @param t the DB transaction to use.
	 */
	public Session(int sessionId, Transaction t) throws InvalidSessionIdException, TransactionException {
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			stmt = t.prepareStatement("SELECT user_id, language, secret FROM systemsession WHERE id = ?");
			stmt.setInt(1, sessionId);
			result = stmt.executeQuery();
			if (result.first()) {
				this.userId = result.getInt("user_id");
				this.languageId = result.getInt("language");
				try {
					this.language = languageId > 0 ? new CNDictionary(languageId).asLanguage() : null;
				} catch (NodeException e) {
					throw new TransactionException("Error while checking session", e);
				}
				this.sessionSecret = result.getString("secret");
			} else {
				throw new InvalidSessionIdException(Integer.toString(sessionId));
			}
		} catch (SQLException e) {
			throw new TransactionException("Error while checking session", e);
		} finally {
			t.closeResultSet(result);
			t.closeStatement(stmt);
		}
		this.sessionId = sessionId;
	}

	/**
	 * Create a new session for user -1. If a sessionSecret is given, it will
	 * use it, otherwise a new session secret is generated
	 * @param user system user
	 * @param ip IP address of the request
	 * @param userAgent user agent of the request
	 * @param sessionSecret
	 *            session secret to use, may be null
	 * @throws NodeException
	 */
	public Session(SystemUser user, String ip, String userAgent, String sessionSecret, int sid) throws NodeException {
		this.userId = ObjectTransformer.getInt(user.getId(), -1);
		// if no session secret was given, we need to create one
		if (ObjectTransformer.isEmpty(sessionSecret)) {
			this.sessionSecret = createSessionSecret();
		} else {
			this.sessionSecret = sessionSecret;
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;
		String val = "";

		try {
			// get the last session of the user
			pst = t.prepareStatement("SELECT ip, agent, cookie, since, language, val FROM systemsession WHERE user_id = ? ORDER BY since DESC LIMIT 1");
			pst.setInt(1, this.userId);
			res = pst.executeQuery();
			if (res.first()) {
				this.languageId = UserLanguageFactory.getById(res.getInt("language"), true).getId();
				val = res.getString("val");
			} else {
				this.languageId = 1;
			}
			this.language = new CNDictionary(languageId).asLanguage();
			t.closeResultSet(res);
			res = null;
			t.closeStatement(pst);
			pst = null;

			int newSid = sid;
			if (sid > 0) {
				// We have to make sure, that the sid that was passed
				// actually belongs to this client (check the secret).
				pst = t.prepareStatement("SELECT secret FROM systemsession WHERE id = ?");
				pst.setInt(1, sid);
				res = pst.executeQuery();

				// If the sid could not be found or the secret
				// doesn't match, we ignore the passed sid
				if (!res.first() || !res.getString("secret").equals(sessionSecret)) {
					newSid = 0;
				}
				t.closeResultSet(res);
				res = null;
				t.closeStatement(pst);
				pst = null;
			}

			if (newSid == 0) {
				// Create a new session
				pst = t.prepareInsertStatement("INSERT INTO systemsession (secret, user_id, ip, agent, since, language, val) VALUES (?, ?, ?, ?, ?, ?, ?)");
				pst.setString(1, this.sessionSecret);
				pst.setInt(2, this.userId);
				pst.setString(3, ip);
				pst.setString(4, userAgent);
				pst.setInt(5, t.getUnixTimestamp());
				pst.setInt(6, this.languageId);
				pst.setString(7, val);
				pst.execute();

				// get the generated keys
				res = pst.getGeneratedKeys();
				if (res.first()) {
					this.sessionId = res.getInt(1);
				} else {
					throw new NodeException("Error while generating new session: Could not get sid");
				}
			} else {
				// Use an existing session
				pst = t.prepareUpdateStatement("UPDATE systemsession SET user_id=?, ip=?, agent=?, since=?, language=?, val=? WHERE id=?");
				pst.setInt(1, this.userId);
				pst.setString(2, ip);
				pst.setString(3, userAgent);
				pst.setInt(4, t.getUnixTimestamp());
				pst.setInt(5, this.languageId);
				pst.setString(6, val);
				pst.setInt(7, newSid);
				pst.executeUpdate();

				this.sessionId = sid;
			}

			// log the login
			ActionLogger.logCmd(ActionLogger.LOGIN, SystemUser.TYPE_SYSTEMUSER, this.userId, t.getUnixTimestamp(), "sid(" + this.sessionId + ")");
		} catch (SQLException e) {
			throw new NodeException("Error while generating new session", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Do a logout of the current session
	 * @throws NodeException
	 */
	public void logout() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		DBUtils.executeUpdate("UPDATE systemsession SET secret = ? WHERE id = ? AND user_id = ?", new Object[] { "", sessionId, userId });

		// log the logout
		ActionLogger.logCmd(ActionLogger.LOGOUT, SystemUser.TYPE_SYSTEMUSER, this.userId, t.getUnixTimestamp(), "sid(" + this.sessionId + ")");
	}

	/**
	 * Do a logout of all sessions that belong
	 * to the users session secret
	 * @throws NodeException
	 */
	public void logoutAllSessions() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;

		try {
			pst = t.prepareUpdateStatement("UPDATE systemsession SET secret = ? WHERE secret = ?");
			pst.setString(1, "");
			pst.setString(2, this.getSessionSecret());
			pst.executeUpdate();

			// log the logout
			ActionLogger.logCmd(ActionLogger.LOGOUT, SystemUser.TYPE_SYSTEMUSER, this.userId, t.getUnixTimestamp(), "sid(" + this.sessionId + ", all sessions)");
		} catch (SQLException e) {
			throw new NodeException("Error while performing logout", e);
		} finally {
			t.closeStatement(pst);
		}
	}

	/**
	 * Move the given character into the "human readable" area of characters
	 * This method was migrated from the old (undocumented) PHP code
	 * @param r character
	 * @return another character
	 */
	protected static char calc(char r) {
		if (r < 10) {
			return (char) (r + 48);
		} else if (r < 36) {
			return (char) (r + 55);
		} else {
			return (char) (r + 61);
		}
	}

	/**
	 * Do some magic calculation and return a string
	 * This method was migrated from the old (undocumented) PHP code
	 * @param n a number
	 * @param base base
	 * @return a string
	 */
	protected static String divide(int n, int base) {
		int r = 0;
		StringBuffer sid = new StringBuffer();

		while (n != 0) {
			r = n % base;
			sid.append(calc((char) r));
			n = (int) Math.floor(n / base);
		}
		return sid.toString();
	}

	/**
	 * Create a new session secret
	 * @return session secret
	 */
	public static String createSessionSecret() {
		int base = 62;
		String z = Long.toString(System.currentTimeMillis());
		StringBuffer a = new StringBuffer();
		StringBuffer b = new StringBuffer();

		for (int i = 0; i < z.length() - 1; i += 2) {
			a.append(z.substring(i, i + 1));
			b.append(z.substring(i + 1, i + 2));
		}
		StringBuffer sid = new StringBuffer();

		sid.append(divide(Integer.parseInt(a.toString()), base));
		sid.append(divide(Integer.parseInt(b.toString()), base));
		Random random = new Random();

		for (int i = sid.length(); i < 15; i++) {
			sid.append(calc((char) random.nextInt(base)));
		}

		return sid.toString();
	}

	/**
	 * Schedule cleaning of system sessions (every minute)
	 * @throws NodeException
	 */
	public static void scheduleSessionCleaning() throws NodeException {
		if (sessionCleaningFuture == null || sessionCleaningFuture.isDone()) {
			sessionCleaningFuture = SimpleScheduler.getExecutor("session-cleanup").scheduleWithFixedDelay(() -> cleanOldSessions(), 0, 1, TimeUnit.MINUTES);
		}
	}

	/**
	 * Check whether the session cleaning job is running.
	 *
	 * @return {@code true} when the session cleaning job is running, and {@code false} otherwise.
	 */
	public static boolean sessionCleaningActive() {
		return sessionCleaningFuture != null && !sessionCleaningFuture.isDone();
	}

	/**
	 * Clean old sessions for all users.
	 * This method will - for all users - remove all systemsessions with a since that is older than the allowed session age,
	 * only the last of those sessions will be kept
	 */
	protected static void cleanOldSessions() {
		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			int sessionAge = ObjectTransformer.getInt(t.getNodeConfig().getDefaultPreferences().getProperty("session_age"), 3600);
			final int allowedSince = t.getUnixTimestamp() - sessionAge;
			final Map<Integer, Integer> maxSincePerUser = new HashMap<>();

			// clean old sessions
			DBUtils.executeStatement(
					"SELECT user_id, MAX(since) maxsince, COUNT(since) count FROM systemsession WHERE since < ? GROUP BY user_id HAVING count > 1",
					new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, allowedSince);
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								maxSincePerUser.put(rs.getInt("user_id"), rs.getInt("maxsince"));
							}
						}
					});
			for (Map.Entry<Integer, Integer> entry : maxSincePerUser.entrySet()) {
				DBUtils.deleteWithPK("systemsession", "id", "user_id = ? AND since < ?", new Object[] {entry.getKey(), entry.getValue()});
			}

			DBUtils.updateWithPK("systemsession", "id", "secret = ?", new Object[] { "" }, "secret != ? AND since < ?",
					new Object[] { "", allowedSince });

			trx.success();
		} catch (NodeException e) {
			NodeLogger.getNodeLogger(Session.class).error("Error while cleaning old sessions", e);
		}
	}

	public int getSessionId() {
		return sessionId;
	}

	public int getUserId() {
		return userId;
	}

	/**
	 * @return the language id for this session. This will be the id for
	 * the language which the user selected in the user preferences.
	 */
	public int getLanguageId() {
		return languageId;
	}

	public String getSessionSecret() {
		return sessionSecret;
	}

	/**
	 * Touch the system session by setting the column since to the transaction timestamp
	 * @throws NodeException
	 */
	public void touch() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		DBUtils.executeUpdate("UPDATE systemsession SET since = ? WHERE id = ? AND secret = ?", new Object[] {t.getUnixTimestamp(), sessionId, sessionSecret});
	}

	@Override
	public Language getLanguage() {
		return language;
	}
}
