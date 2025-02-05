/*
 * @author tobiassteiner
 * @date Feb 6, 2011
 * @version $Id: SessionToken.java,v 1.1.2.3 2011-02-21 06:14:49 tobiassteiner Exp $
 */
package com.gentics.contentnode.factory;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import com.gentics.lib.util.QueryStringUtils;

/**
 * Note: this implementation depends strongly on how mysession_create_token()
 * (PHP) is implemented. Any changes in this class may require changes in
 * mysession_create_token() and related functions.
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
	 * The name of the "sid" query parameter for GCN backend URLs,
	 * that either contains the session id or a session token string. 
	 */
	public static final String SESSION_ID_QUERY_PARAM_NAME = "sid"; 

	/**
	 * The length of the secret part in a session token string.
	 * @see mysession_create_secret() (PHP).
	 */
	protected static final int SESSION_TOKEN_SECRET_LENGTH = 15;
    
	/**
	 * The primary key into the systemsession table.
	 */
	protected final int sessionId;
    
	/**
	 * The session secret this instance was created with.
	 */
	protected final String sessionSecret;
    
	/**
	 * The session secret retrieved from the session token string that was
	 * used to construct this instance.
	 */
	protected final String tokenSecret;

	/**
	 * Like {@link #SessionToken(String, String)} but without an additional secret. 
	 * 
	 * @param sessionId
	 *   If this parameter is of the primary key form, the this token
	 *   can't authenticate a user.
	 *   If this parameter is a session token string, the user can be
	 *   authenticated.
	 */
	public SessionToken(String sessionId) throws InvalidSessionIdException {
		this(sessionId, null);
	}
    
	/**
	 * @param sessionId may either be a string representation of a
	 *   primary key into the systemsession table (a java int), or a
	 *   session token string.
	 *   
	 *   Session token strings are created with {@link #toString()} or with
	 *   mysession_create_token() (PHP). 
	 *   Session token strings can contain the session Id (the primary key form)
	 *   and the session secret.
	 *   
	 * @param sessionSecret an additional secret that can be used to
	 *   authenticate a user for the session identified with this
	 *   instance.
	 *   If the sessionId is a session token string, both, the secret in
	 *   the session token string as well as the given sessionSecret will
	 *   be used to authenticate a user for the session identified with
	 *   this instance.
	 *   
	 * @throws InvalidSessionIdException if the given string represents
	 *   a session token string with an invalid session id, or if the given
	 *   sessionId is null or if the given sessionId parameter is neither
	 *   of the primary key form, nor a session token string created with
	 *   {@link #toString()} or mysession_create_token() (PHP). 
	 */
	public SessionToken(String sessionId, String sessionSecret) throws InvalidSessionIdException {
		if (null == sessionId) {
			throw new InvalidSessionIdException(sessionId);
		}

		int tokenId;
		String tokenSecret;

		try {
			tokenId = Integer.parseInt(sessionId);
			tokenSecret = null;
		} catch (NumberFormatException e) {
			if (!looksLikeToken(sessionId)) {
				throw new InvalidSessionIdException(sessionId, e);
			}
			try {
				tokenId = parseIdFromTokenString(sessionId);
			} catch (NumberFormatException e2) {
				throw new InvalidSessionIdException(sessionId, e2);
			}
			tokenSecret = parseSecretFromTokenString(sessionId);
		}
		this.sessionId = tokenId;
		this.sessionSecret = sessionSecret;
		this.tokenSecret = tokenSecret;
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
		this(request.getParameter(SESSION_ID_QUERY_PARAM_NAME), getSessionSecretFromRequestCookie(request));        
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

	public SessionToken(int sessionId, String sessionSecret) {
		this.sessionId = sessionId;
		this.sessionSecret = sessionSecret;
		this.tokenSecret = null;
	}

	public int getSessionId() {
		return sessionId;
	}

	/**
	 * @return true if this token contains a session secret that can
	 *   be used to authenticate a user for a session.
	 */
	public boolean canAuthenticate() {
		return null != this.sessionSecret || null != this.tokenSecret;
	}
    
	/**
	 * @return true if this token authenticates the given session.
	 */
	public boolean authenticates(Session session) {
		return canAuthenticate() && session.getUserId() > 0 && getSessionId() == session.getSessionId()
				&& ((null != sessionSecret && sessionSecret.equals(session.getSessionSecret()))
				|| (null != tokenSecret && tokenSecret.equals(session.getSessionSecret())));
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
	 * Note: depends strongly on how mysession_create_token() (PHP) is implemented. 
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
	 * Note: depends strongly on how mysession_create_token() (PHP) is implemented. 
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

	/**
	 * Injects this token as the "sid" parameter into the given queryString.
	 * 
	 * @param queryString an URL query string (without the first '?').
	 * 
	 * @param encoding the encoding to use to decode/encode query parameter
	 *   names and values. (e.g. "UTF-8").
	 * 
	 * @return a query string with the "sid" query parameter replaced with
	 *   a valid session token string, so that the "sid" query parameter can be
	 *   used to identify and authenticate a session. This may either add
	 *   a new "sid" parameter, but more likely replaces the already present
	 *   "sid" parameter, which is most likely the session ID. 
	 * @throws UnsupportedEncodingException 
	 */
	public String injectIntoQueryString(String queryString, String encoding) throws UnsupportedEncodingException {
		String sessionToken = this.toString();
		Map<String, String[]> params = QueryStringUtils.parseQueryString(queryString, encoding);

		params.put(SESSION_ID_QUERY_PARAM_NAME, new String[] { sessionToken });
		return QueryStringUtils.buildQueryString(params, encoding);
	}
    
	/**
	 * Note: depends strongly on how mysession_create_token() (PHP) is implemented. 
	 * Will return a session token string  that can be used to identify a session.
	 * If this token returns true for {@link #canAuthenticate()}, the return session
	 * token string will contain the secret too, so that the session can be
	 * authenticated.
	 * 
	 * This value can be passed to {@link #SessionToken(String)} which will recreate
	 * this token, except that the new instance will only have one secret to choose
	 * from, except the usualy two (the session token string and the session secret
	 * cookie).
	 * 
	 * TODO: maybe we should only store a single session secret and decide at
	 * construction whether it is the session secret cookie or the secret in the
	 * session token string we use.
	 * 
	 * Since a {@link SessionToken} instance has two sources for the session secret,
	 * the session token string  in string form as well as the session secret cookie,
	 * the former will be used if it is available, otherwise the latter will be used.
	 */
	@Override
	public String toString() {
		int maxCharsInPositiveInt = 10;
		StringBuilder sessionToken = new StringBuilder(SESSION_TOKEN_SECRET_LENGTH + maxCharsInPositiveInt);

		sessionToken.append(this.sessionId);
		if (null != this.tokenSecret) {
			// prefer the session secret in the session token string. 
			sessionToken.append(this.tokenSecret);
		} else if (null != this.sessionSecret) {
			// fall-back to the session secret cookie.
			sessionToken.append(this.sessionSecret);
		} else {// otherwise build a session token string without
			// a secret.
		}
		return sessionToken.toString();
	}
}
