package com.gentics.contentnode.factory;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Implementation of a Session Token
 */
public class SessionToken {
	/**
	 * The name for the cookie that contains the session secret.
	 * This value is defined in global.inc.php.
	 * This value is hardcoded in the GCNAuthenticationManager because we don't have
	 * access to the GCN configuration in portal node. This value is also hardcoded
	 * in the GCNProxyServlet because we don't have access to the GCN config there.
	 * We also hardcode it here, although we do have access to the GCN configuration
	 * here, simply for convenience, because many tests would fail otherwise. 
	 */
	public static final String SESSION_SECRET_COOKIE_NAME = "GCN_SESSION_SECRET";

	/**
	 * The encoding to use for the query string of a request.
	 * Can be used with {@link #injectIntoQueryString(String, String)}.
	 * I suppose this encoding will just work. I'm not sure how to determine
	 * the correct encoding to use.
	 */
	public static final String SANE_DEFAULT_QUERY_STRING_ENCODING = "UTF-8";

	/**
	 * The length of the secret part in a session token string.
	 * @see mysession_create_secret() (PHP).
	 */
	protected static final int SESSION_TOKEN_SECRET_LENGTH = 15;

	/**
	 * Token value
	 */
	protected final String token;

	/**
	 * The primary key into the systemsession table.
	 */
	protected final int sessionId;

	/**
	 * The session secret this instance was created with.
	 */
	protected final String sessionSecret;

	/**
	 * Create an instance with the given session secret
	 * @param token session secret
	 */
	public SessionToken(String token) throws InvalidSessionIdException {
		if (null == token) {
			throw new InvalidSessionIdException(token);
		}

		if (!looksLikeToken(token)) {
			throw new InvalidSessionIdException(token);
		}
		try {
			this.sessionId = parseIdFromTokenString(token);
			this.sessionSecret = parseSecretFromTokenString(token);
			this.token = token;
		} catch (NumberFormatException e2) {
			throw new InvalidSessionIdException(token, e2);
		}
	}

	/**
	 * Like {@link #SessionToken(String, String)} but constructs a new instance
	 * with the session id and session secret retrieved from the given request
	 * (from the "sid" query parameter and session secret cookie).
	 * 
	 * @param request assumed to be a request to the GCN backend. Obviously,
	 *   if this isn't a request to the GCN backend, the session secret cookie,
	 *   and the "sid" query parameter will either not be present, or will
	 *   mean something different, so it makes no sense to call this method
	 *   with anything else. 
	 */
	public SessionToken(HttpServletRequest request) throws InvalidSessionIdException {
		// note: getParemeter() retrieves query parameters as well as
		// from-posted values.
		this(getSessionSecretFromRequestCookie(request));        
	}

	/**
	 * @return the cookie value of the session secret retrieved from the given request,
	 *   or null if there is no session secret cookie. 
	 */
	public static String getSessionSecretFromRequestCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();

		// request may not have cookies
		if (null == cookies) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (SESSION_SECRET_COOKIE_NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public int getSessionId() {
		return sessionId;
	}

	/**
	 * @return true if this token contains a session secret that can
	 *   be used to authenticate a user for a session.
	 */
	public boolean canAuthenticate() {
		return StringUtils.isNotBlank(sessionSecret);
	}

	public Session getSession() throws NodeException {
		return DBSession.load(this).orElse(null);
	}

	/**
	 * Note: depends strongly on how mysession_create_token() (PHP) is implemented. 
	 * 
	 * @param sessionToken a string that may be either the primary key into
	 *   the sytemsession table, or a session token string.
	 *   
	 * @return true if the given sessionId is of the session token string form, rather
	 *   than the primary key form.
	 */
	private static boolean looksLikeToken(String sessionToken) {
		// a session token string that contains a session ID and a
		// session secret must be at least as long as the secret.
		return sessionToken.length() > SESSION_TOKEN_SECRET_LENGTH;
	}

	/**
	 * Parse the session ID ouf of the token 
	 * 
	 * @param sessionToken  the session token string that contains the id.
	 * @return the session identifier - the primary key into the systemsession table.
	 * @throws IllegalArgumentException if the given session token string can't be parsed.
	 * @throws NumberFormatException if the session token string contains
	 *   a session id that isn't a valid int.
	 */
	public static int parseIdFromTokenString(String sessionToken) {
		if (!looksLikeToken(sessionToken)) {
			throw new IllegalArgumentException("Not a session token: `" + sessionToken + "'");
		}
		int idPartLength = sessionToken.length() - SESSION_TOKEN_SECRET_LENGTH;

		return Integer.parseInt(sessionToken.substring(0, idPartLength));
	}

	/**
	 * Parse the session secret out of the token 
	 * 
	 * @param sessionToken the session token string that contains the secret.
	 * @return the session secret from the given token.
	 * @throws IllegalArgumentException if the given session token string can't be parsed.
	 */
	public static String parseSecretFromTokenString(String sessionToken) {
		if (!looksLikeToken(sessionToken)) {
			throw new IllegalArgumentException("Not a session token: `" + sessionToken + "'");
		}
		return sessionToken.substring(sessionToken.length() - SESSION_TOKEN_SECRET_LENGTH);
	}

	@Override
	public String toString() {
		return token;
	}
}
