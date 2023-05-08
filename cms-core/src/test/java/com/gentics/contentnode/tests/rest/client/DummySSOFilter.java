package com.gentics.contentnode.tests.rest.client;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.SystemUser;

/**
 * Dummy SSO Filter
 */
@Provider
@PreMatching
public class DummySSOFilter implements ContainerRequestFilter {
	/**
	 * Login of the test user
	 */
	public final static String LOGIN = "test";

	/**
	 * Password of the test user
	 */
	public final static String PASSWORD = "test";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		try {
			Trx.operate(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				SystemUser systemUser = ((SystemUserFactory) t.getObjectFactory(SystemUser.class)).getSystemUser(LOGIN, null, false);

				Session session = new Session(systemUser, "localhost", "Dummy User Agent", null, 0);
				UriBuilder builder = requestContext.getUriInfo().getRequestUriBuilder();
				builder.replaceQueryParam(SessionToken.SESSION_ID_QUERY_PARAM_NAME, session.getSessionId());
				requestContext.setRequestUri(builder.build());
				requestContext.getHeaders().add(HttpHeaders.COOKIE,
						new NewCookie(SessionToken.SESSION_SECRET_COOKIE_NAME, session.getSessionSecret()).toString());
			});
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
