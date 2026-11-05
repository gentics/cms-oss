package com.gentics.contentnode.mcp.auth;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.ApiTokenFactory;
import com.gentics.contentnode.auth.ResolvableApiTokenDataModel;
import com.gentics.contentnode.factory.ApiTokenSession;
import com.gentics.contentnode.factory.DBSession;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Trx;
import com.gentics.lib.log.NodeLogger;

/**
 * Resolves {@link McpRequestCredentials} extracted from an incoming MCP HTTP request into a real
 * CMS {@link Session}.
 *
 * <p>
 * This is the counterpart, for the {@code /mcp} servlet, of
 * {@code AuthenticationRequestFilter#tryApiToken()}/{@code #trySessionSecretSession()} - same
 * precedence (API token first, then session-secret cookie), same lookups
 * ({@code ApiTokenFactory#hash}/{@code #load} + {@link ApiTokenSession}, respectively
 * {@link SessionToken} + {@link DBSession#load}) - since that filter is bound to the Jersey/JAX-RS
 * pipeline and never runs for {@code /mcp}.
 * </p>
 */
public final class McpAuthenticator {
	private static final NodeLogger logger = NodeLogger.getNodeLogger(McpAuthenticator.class);

	private McpAuthenticator() {
	}

	/**
	 * Resolve the given credentials into a session. Runs its own short-lived transaction (as the
	 * CMS system user) for the DB lookups involved - the same pattern
	 * {@code AuthenticationRequestFilter}/{@code ApiTokenSessionClosure} use for the same purpose.
	 * @param credentials credentials extracted from the request
	 * @return resolved session, or empty if neither credential resolves to a valid session
	 */
	public static Optional<Session> resolve(McpRequestCredentials credentials) {
		if (credentials.isEmpty()) {
			return Optional.empty();
		}

		try {
			return Trx.supply(() -> resolveInTransaction(credentials));
		} catch (NodeException e) {
			logger.error("Error while resolving MCP request credentials", e);
			return Optional.empty();
		}
	}

	private static Optional<Session> resolveInTransaction(McpRequestCredentials credentials) throws NodeException {
		Optional<Session> apiTokenSession = tryApiToken(credentials);
		if (apiTokenSession.isPresent()) {
			return apiTokenSession;
		}

		return trySessionSecret(credentials);
	}

	/**
	 * Try authenticating with an API Token. Mirrors
	 * {@code AuthenticationRequestFilter#tryApiToken()}.
	 */
	private static Optional<Session> tryApiToken(McpRequestCredentials credentials) throws NodeException {
		Optional<String> apiToken = credentials.apiToken().filter(StringUtils::isNotBlank);
		if (apiToken.isEmpty()) {
			return Optional.empty();
		}

		String tokenHash = ApiTokenFactory.hash(apiToken.get());
		Optional<ResolvableApiTokenDataModel> optToken = ApiTokenFactory.load(tokenHash);

		if (optToken.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(new ApiTokenSession(optToken.get()));
	}

	/**
	 * Try authentication with the session secret. Mirrors
	 * {@code AuthenticationRequestFilter#trySessionSecretSession()}.
	 */
	private static Optional<Session> trySessionSecret(McpRequestCredentials credentials) throws NodeException {
		Optional<String> sessionSecret = credentials.sessionSecret().filter(StringUtils::isNotBlank);
		if (sessionSecret.isEmpty()) {
			return Optional.empty();
		}

		try {
			SessionToken token = new SessionToken(sessionSecret.get());
			Optional<DBSession> optSession = DBSession.load(token);
			if (optSession.isPresent()) {
				optSession.get().touch();
			}
			return optSession.map(Session.class::cast);
		} catch (InvalidSessionIdException e) {
			return Optional.empty();
		}
	}
}
