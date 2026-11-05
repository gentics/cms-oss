package com.gentics.contentnode.mcp.auth;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.runtime.ConfigurationValue;

import io.modelcontextprotocol.server.transport.ServerTransportSecurityException;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityValidator;

/**
 * Optional, coarse-grained guard for the MCP endpoint, at the transport level: rejects a request
 * with HTTP 401 if it carries neither an {@code Authorization: Bearer} header nor the CMS session
 * cookie, before the request even reaches a tool call.
 *
 * <p>
 * <b>This only checks for the presence of a credential, not its validity.</b> Actually resolving
 * a credential into a real CMS {@link com.gentics.contentnode.factory.Session} requires a DB
 * lookup (see {@link McpAuthenticator}), and this validator is called synchronously by the
 * transport, before any transaction is available (see
 * {@code HttpServletStreamableServerTransportProvider#doPost}/{@code #doGet}/{@code #doDelete}),
 * so it deliberately does not attempt one. An invalid or expired token/cookie still passes this
 * check; it is only rejected later, inside the tool call itself
 * ({@code com.gentics.contentnode.mcp.AbstractMcpTool}), which always rejects an unauthenticated
 * call for a tool that requires one - independent of, and regardless of, this validator's
 * configuration.
 * </p>
 *
 * <p>
 * Controlled by {@link ConfigurationValue#MCP_REQUIRE_AUTH}, <b>disabled by default</b> - so
 * existing setups (e.g. connecting with the MCP Inspector without configuring any credentials)
 * keep working unless explicitly opted into the stricter behavior. When disabled, this validator
 * behaves exactly like {@link ServerTransportSecurityValidator#NOOP}.
 * </p>
 */
public final class CmsMcpSecurityValidator implements ServerTransportSecurityValidator {
	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String COOKIE_HEADER = "Cookie";

	/**
	 * Matches a raw {@code Cookie} header value that contains the CMS session cookie, e.g.
	 * {@code "other=x; GCN_SESSION_SECRET=abc; more=y"}. A simple presence check, not a full
	 * cookie-header parse - sufficient here since only presence, not the value, matters.
	 */
	private static final Pattern SESSION_COOKIE = Pattern
			.compile("(^|.*;\\s*)" + Pattern.quote(SessionToken.SESSION_SECRET_COOKIE_NAME) + "=");

	@Override
	public void validateHeaders(Map<String, List<String>> headers) throws ServerTransportSecurityException {
		if (!Boolean.parseBoolean(ConfigurationValue.MCP_REQUIRE_AUTH.get())) {
			return;
		}

		if (hasBearerToken(headers) || hasSessionCookie(headers)) {
			return;
		}

		throw new ServerTransportSecurityException(401,
				"Authentication required: send either an 'Authorization: Bearer <api token>' header or the '"
						+ SessionToken.SESSION_SECRET_COOKIE_NAME + "' session cookie");
	}

	private boolean hasBearerToken(Map<String, List<String>> headers) {
		return headerValues(headers, AUTHORIZATION_HEADER).stream()
				.anyMatch(value -> CmsMcpContextExtractor.BEARER_TOKEN.matcher(value).matches());
	}

	private boolean hasSessionCookie(Map<String, List<String>> headers) {
		return headerValues(headers, COOKIE_HEADER).stream().anyMatch(value -> SESSION_COOKIE.matcher(value).find());
	}

	/**
	 * Look the given header name up case-insensitively (the SDK's header map preserves whatever
	 * casing {@code HttpServletRequest#getHeaderNames()} returned, see
	 * {@code HttpServletRequestUtils#extractHeaders}).
	 */
	private List<String> headerValues(Map<String, List<String>> headers, String name) {
		return headers.entrySet().stream().filter(entry -> name.equalsIgnoreCase(entry.getKey()))
				.flatMap(entry -> entry.getValue().stream()).filter(value -> value != null).toList();
	}
}
