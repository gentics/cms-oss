package com.gentics.contentnode.rest.filters;

import static com.gentics.contentnode.factory.Trx.supply;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.ApiTokenFactory;
import com.gentics.contentnode.auth.ResolvableApiTokenDataModel;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.ApiTokenSession;
import com.gentics.contentnode.factory.DBSession;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.log.NodeLogger;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

/**
 * Filter implementation that checks authentication with sid/secret
 */
@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationRequestFilter implements ContainerRequestFilter {
	protected final static Pattern BEARER_TOKEN = Pattern.compile("Bearer\s(.*)");

	@Context
	UriInfo uriInfo;

	@Context
	HttpHeaders headers;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		try {
			Optional<? extends Session> optSession = supply(() -> tryApiToken());

			if (optSession.isEmpty()) {
				optSession = supply(() -> trySessionSecretSession(requestContext));
			}

			if (optSession.isPresent()) {
				ContentNodeHelper.setSession(optSession.get());
			} else {
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("").build());
			}
		} catch (InvalidSessionIdException e) {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("").build());
		} catch (NodeException e) {
			NodeLogger.getNodeLogger(getClass()).error(e);
			requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(MiscUtils.serverError()).build());
		}
	}

	/**
	 * Try authenticating with an API Token. If a valid API Token is found as Authorization: Bearer header, a {@link Session} is created and returned.
	 * Otherwise an empty optional is returned
	 * @return optional session
	 * @throws NodeException
	 */
	private Optional<? extends Session> tryApiToken() throws NodeException {
		Optional<String> optBearerToken = Optional.ofNullable(headers.getHeaderString("Authorization")).map(auth -> BEARER_TOKEN.matcher(auth))
				.filter(Matcher::matches).map(m -> m.group(1));

		if (optBearerToken.isPresent()) {
			String token = optBearerToken.get();
			String tokenHash = ApiTokenFactory.hash(token);
			Optional<ResolvableApiTokenDataModel> optToken = ApiTokenFactory.load(tokenHash);

			if (optToken.isPresent()) {
				return Optional.of(new ApiTokenSession(optToken.get()));
			}
		}

		return Optional.empty();
	}

	/**
	 * Try authentication with session secret sent as cookie. If a valid session secret is found, a {@link Session} is returned.
	 * Otherwise an empty optional is returned
	 * @param requestContext request context
	 * @return optional session
	 * @throws NodeException
	 */
	private Optional<? extends Session> trySessionSecretSession(ContainerRequestContext requestContext) throws NodeException {
		String sessionSecret = getSessionSecret();

		if (!StringUtils.isEmpty(sessionSecret)) {
			SessionToken token = new SessionToken(sessionSecret);
			Optional<DBSession> optSession = DBSession.load(token);
			if (optSession.isPresent()) {
				optSession.get().touch();
			}
			return optSession;
		}

		return Optional.empty();
	}

	/**
	 * Extract the session secret from the cookies
	 * @return session secret or null
	 */
	private String getSessionSecret() {
		Map<String, Cookie> cookies = headers.getCookies();

		// cookies may be null
		if (null == cookies) {
			return null;
		}
		Cookie sessionSecretCookie = cookies.get(SessionToken.SESSION_SECRET_COOKIE_NAME);

		if (null == sessionSecretCookie) {
			return null;
		}
		return sessionSecretCookie.getValue();
	}
}
