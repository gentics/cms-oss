package com.gentics.contentnode.mcp.auth;

import java.util.Optional;

/**
 * Authentication credentials extracted from an incoming MCP HTTP request (see
 * {@link CmsMcpContextExtractor}), carried down into a tool call via
 * {@link io.modelcontextprotocol.common.McpTransportContext} under {@link #CONTEXT_KEY}, and
 * resolved into a real CMS {@link com.gentics.contentnode.factory.Session} by
 * {@link McpAuthenticator}.
 *
 * <p>
 * Mirrors the two credential shapes {@code AuthenticationRequestFilter} accepts for the regular
 * {@code /rest/*} endpoints: an API token, sent as an {@code Authorization: Bearer <token>}
 * header, and a session secret, sent as the {@code GCN_SESSION_SECRET} cookie.
 * </p>
 *
 * @param apiToken raw API token (the bearer token value, not yet hashed), if the request carried
 * one
 * @param sessionSecret raw session secret, if the request carried the CMS session cookie
 */
public record McpRequestCredentials(Optional<String> apiToken, Optional<String> sessionSecret) {
	/**
	 * Key under which an instance of this record is stored in the
	 * {@link io.modelcontextprotocol.common.McpTransportContext} of an incoming MCP request.
	 */
	public static final String CONTEXT_KEY = "com.gentics.contentnode.mcp.credentials";

	/**
	 * Credentials instance carrying neither an API token nor a session secret, e.g. for a request
	 * that provided none, or as a fallback if the transport context does not carry a
	 * {@link McpRequestCredentials} instance at all (which should not normally happen, since
	 * {@link CmsMcpContextExtractor} always sets one).
	 */
	public static final McpRequestCredentials EMPTY = new McpRequestCredentials(Optional.empty(), Optional.empty());

	/**
	 * @return true iff neither an API token nor a session secret is present
	 */
	public boolean isEmpty() {
		return apiToken.isEmpty() && sessionSecret.isEmpty();
	}
}
