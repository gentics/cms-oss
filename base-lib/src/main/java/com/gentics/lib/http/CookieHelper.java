package com.gentics.lib.http;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for secure Cookies. Used to work around Servlet 2.5 limitations concerning the "HttpOnly" cookie attribute
 *
 * @author Wolfgang Illmeyer
 *
 */
public final class CookieHelper {

	/**
	 * Set a cookie
	 *
	 * @param name
	 *            name of the cookie (only ASCII, no whitespace, no ";", no "=" and no ",")
	 * @param value
	 *            value of the cookie. Must be Version 0 compatible (only ASCII, no whitespace, no ";" and no ",")
	 * @param path
	 *            path of the cookie
	 * @param maxAge
	 *            MaxAge value of the cookie. Use 0 to delete, null for session cookies
	 * @param secure
	 *            set secure flag of the cookie
	 * @param httpOnly
	 *            set the HttpOnly flag of the cookie
	 * @param res
	 *            the response where to set the cookie
	 *
	 * @deprecated Use the setCookie version with SameSite parameter instead.
	 */
	@Deprecated
	public static void setCookie(String name, String value, String path, Integer maxAge, boolean secure, boolean httpOnly, HttpServletResponse res) {
		setCookie(name, value, path, maxAge, secure, httpOnly, SameSite.LAX, res);
	}

	/**
	 * Set a cookie
	 *
	 * @param name
	 *            name of the cookie (only ASCII, no whitespace, no ";", no "=" and no ",")
	 * @param value
	 *            value of the cookie. Must be Version 0 compatible (only ASCII, no whitespace, no ";" and no ",")
	 * @param path
	 *            path of the cookie
	 * @param maxAge
	 *            MaxAge value of the cookie. Use 0 to delete, null for session cookies
	 * @param secure
	 *            set secure flag of the cookie
	 * @param httpOnly
	 *            set the HttpOnly flag of the cookie
	 * @param sameSite
	 *            set the SameSite attribute
	 * @param res
	 *            the response where to set the cookie
	 */
	public static void setCookie(String name, String value, String path, Integer maxAge, boolean secure, boolean httpOnly, SameSite sameSite, HttpServletResponse res) {
		StringBuilder sb = new StringBuilder();

		sb.append(name).append('=').append(value).append("; Path=").append(path);
		if (maxAge != null) {
			sb.append("; Version=1; Max-Age=").append(maxAge);
		}
		if (httpOnly) {
			sb.append("; HttpOnly");
		}
		if (secure) {
			sb.append("; Secure");
		}
		if (sameSite != null) {
			sb.append("; SameSite=" + sameSite.value());
		}
		res.addHeader("Set-Cookie", sb.toString());
	}

	/**
	 * SameSite cookie attribute value variants.
	 * 
	 * @author plyhun
	 */
	public static enum SameSite {
		LAX("Lax"),
		STRICT("Strict"),
		NONE("None");

		private final String value;

		private SameSite(String value) {
			this.value = value;
		}

		public String value() {
			return value;
		}

		public static SameSite parse(String s) {
			if (StringUtils.isBlank(s)) {
				return null;
			}
			switch (s.trim().toLowerCase()) {
			case "lax":
				return LAX;
			case "strict":
				return STRICT;
			case "none":
				return NONE;
			}
			throw new IllegalArgumentException("Unsupported value for SameSite cookie attribute: " + s);
		}
	}

	// No subclassing or instancing.
	private CookieHelper() {}
}
