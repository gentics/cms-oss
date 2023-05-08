package com.gentics.contentnode.rest.filters;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ContainerRequest;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Filter implementation that checks authentication with sid/secret
 */
@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationRequestFilter implements ContainerRequestFilter {
	@Context
	UriInfo uriInfo;

	@Context
	HttpHeaders headers;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String sid = getSid(requestContext);
		String sessionSecret = getSessionSecret();

		if (ObjectTransformer.isEmpty(sid)) {
			requestContext.abortWith(
					Response.status(Response.Status.UNAUTHORIZED).entity("sid and session secret required").build());
			return;
		}

		// check whether sid/sessionSecret are valid
		try {
			SessionToken token = new SessionToken(sid, sessionSecret);
			Session session = null;
			try (Trx trx = new Trx(sid, null)) {
				session = trx.getTransaction().getSession();
				if (!token.authenticates(session)) {
					throw new InvalidSessionIdException(sid);
				}

				session.touch();
				trx.success();
			}
			ContentNodeHelper.setSession(session);
		} catch (InvalidSessionIdException e) {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("invalid sid").build());
		} catch (NodeException e) {
			NodeLogger.getNodeLogger(getClass()).error(e);
			requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(MiscUtils.serverError()).build());
		}
	}

	/**
	 * Extract the session id from the URI
	 * @param requestContext request context
	 * @return sid or null
	 */
	private String getSid(ContainerRequestContext requestContext) {
		Optional<String> optionalQueryParamSid = Optional.ofNullable(uriInfo.getQueryParameters().get(SessionToken.SESSION_ID_QUERY_PARAM_NAME))
				.flatMap(values -> values.stream().findFirst());
		if (optionalQueryParamSid.isPresent()) {
			return optionalQueryParamSid.get();
		}

		Optional<String> optionalRefererSid = Optional.ofNullable(headers.getHeaderString("Referer")).map(this::getSidFromReferer);
		if (optionalQueryParamSid.isPresent()) {
			return optionalRefererSid.get();
		}

		// check whether request is multipart/form-data
		if (MediaTypes.typeEqual(requestContext.getMediaType(), MediaType.MULTIPART_FORM_DATA_TYPE) && requestContext.hasEntity() && requestContext instanceof ContainerRequest) {
			ContainerRequest containerRequest = (ContainerRequest) requestContext;
			containerRequest.bufferEntity();
			FormDataMultiPart data = containerRequest.readEntity(FormDataMultiPart.class);
			return Optional.ofNullable(data.getField("sid")).map(FormDataBodyPart::getValue).orElse(null);
		}

		return null;
	}

	/**
	 * Try to get the sid from the referer
	 * @param referer referer
	 * @return SID or null if not found
	 */
	private String getSidFromReferer(String referer) {
		try {
			List<NameValuePair> queryParams = URLEncodedUtils.parse(new URI(referer), "UTF-8");
			return queryParams.stream().filter(pair -> SessionToken.SESSION_ID_QUERY_PARAM_NAME.equals(pair.getName())).map(NameValuePair::getValue)
					.findFirst().orElse(null);
		} catch (URISyntaxException e) {
			return null;
		}
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
