package com.gentics.contentnode.mcp.auth;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.contentnode.factory.SessionToken;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;

/**
 * Extracts CMS authentication credentials from the raw HTTP request of an incoming MCP call
 * (the {@code Authorization: Bearer <api token>} header, and the {@code GCN_SESSION_SECRET}
 * session cookie), and carries them into a {@link McpRequestCredentials} instance so that a tool
 * call handler (see {@code com.gentics.contentnode.mcp.AbstractMcpTool}) can later resolve them
 * into a real CMS {@link com.gentics.contentnode.factory.Session} via {@link McpAuthenticator}.
 *
 * <p>
 * This is the counterpart, for the {@code /mcp} servlet, of what
 * {@code AuthenticationRequestFilter#tryApiToken()}/{@code #trySessionSecretSession()} do for the
 * regular {@code /rest/*} endpoints - except that this class deliberately does <b>no DB access
 * and never throws</b>. It is invoked by the MCP transport
 * ({@code HttpServletStreamableServerTransportProvider#doPost}, etc.) outside of that method's
 * own try/catch, so any exception thrown here would surface to the client as a raw HTTP 500
 * instead of a clean MCP-level error. Actual credential validation only happens later, per tool
 * call - see {@link McpAuthenticator}.
 * </p>
 */
public final class CmsMcpContextExtractor implements McpTransportContextExtractor<HttpServletRequest> {
	/**
	 * Matches an "Authorization: Bearer &lt;token&gt;" header value. Mirrors
	 * {@code AuthenticationRequestFilter#BEARER_TOKEN}.
	 */
	static final Pattern BEARER_TOKEN = Pattern.compile("Bearer\\s(.*)");

	@Override
	public McpTransportContext extract(HttpServletRequest request) {
		McpRequestCredentials credentials = new McpRequestCredentials(extractApiToken(request),
				extractSessionSecret(request));

		return McpTransportContext.create(Map.of(McpRequestCredentials.CONTEXT_KEY, credentials));
	}

	/**
	 * Extract the API token from the request's {@code Authorization} header, if present.
	 */
	private Optional<String> extractApiToken(HttpServletRequest request) {
		String authorization = request.getHeader("Authorization");
		if (authorization == null) {
			return Optional.empty();
		}

		Matcher matcher = BEARER_TOKEN.matcher(authorization);
		return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
	}

	/**
	 * Extract the session secret from the request's {@code GCN_SESSION_SECRET} cookie, if present.
	 */
	private Optional<String> extractSessionSecret(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}

		for (Cookie cookie : cookies) {
			if (SessionToken.SESSION_SECRET_COOKIE_NAME.equals(cookie.getName())) {
				return Optional.of(cookie.getValue());
			}
		}

		return Optional.empty();
	}
}
