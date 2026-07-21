package com.gentics.contentnode.tests.rest.client;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

import com.gentics.contentnode.factory.DBSession;
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

				DBSession session = new DBSession(systemUser, "localhost", "Dummy User Agent", null, 0);
				UriBuilder builder = requestContext.getUriInfo().getRequestUriBuilder();
				requestContext.setRequestUri(builder.build());
				requestContext.getHeaders().add(HttpHeaders.COOKIE,
						new NewCookie(SessionToken.SESSION_SECRET_COOKIE_NAME, session.getCookieValue()).toString());
			});
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
