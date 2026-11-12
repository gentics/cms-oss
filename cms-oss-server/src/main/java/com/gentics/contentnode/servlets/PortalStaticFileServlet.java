package com.gentics.contentnode.servlets;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.DBSession;
import com.gentics.contentnode.factory.InvalidSessionIdException;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.resource.impl.NodePreviewProxyResourceImpl;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.log.NodeLogger;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet that handles requests to static files (/static/...), which are not served from devtool packages.
 * If the request contains the cookie {@link RenderUtils#PORTAL_PREVIEW_NODE_COOKIE} (which is set when a page preview is rendered by a portal),
 * the request contains a valid session (session secret cookie), the user of the session is allowed to view the node and the node has a mesh preview URL set,
 * the request is proxied to the portal.
 * Otherwise, the response is
 * <ul>
 * <li>"401 Unauthorized", if the cookie is set, but the request does not contain a valid session</li>
 * <li>"403 Forbidden", if the user is not allowed to view the node</li>
 * <li>"404 Not Found" in all other cases</li>
 * </ul>
 */
public class PortalStaticFileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(PortalStaticFileServlet.class);

	/**
	 * Lowercase names of request headers, which will not be forwarded to the portal
	 */
	protected final static List<String> IGNORED_REQUEST_HEADERS = List.of("host", "cookie", "content-length", "connection", "keep-alive",
			"transfer-encoding", "upgrade", "te", "trailer", "proxy-authorization");

	/**
	 * Lowercase names of response headers, which will not be forwarded from the portal
	 */
	protected final static List<String> IGNORED_RESPONSE_HEADERS = List.of("set-cookie", "content-length", "connection", "keep-alive",
			"transfer-encoding", "upgrade", "trailer");

	/**
	 * Information about the portal, requests are proxied to
	 */
	protected static record PortalTarget(String nodeName, String baseUrl, boolean insecure) {
	}

	/**
	 * Exception thrown, when a request cannot be proxied to a portal. Contains the status code of the response
	 */
	protected static class NotProxiedException extends Exception {
		private static final long serialVersionUID = 1L;

		/**
		 * Response status code
		 */
		protected final int status;

		/**
		 * Create an instance
		 * @param status response status code
		 */
		public NotProxiedException(int status) {
			this.status = status;
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		forward(request, response, new HttpGet());
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException {
		forward(request, response, new HttpHead());
	}

	/**
	 * Forward the request to the portal of the node, which is identified by the cookie
	 * @param request request
	 * @param response response
	 * @param method request method
	 * @throws IOException
	 */
	protected void forward(HttpServletRequest request, HttpServletResponse response, HttpRequestBase method) throws IOException {
		PortalTarget target;
		try {
			target = getPortalTarget(request);
		} catch (NotProxiedException e) {
			response.sendError(e.status);
			return;
		}

		// the base URL ends with a slash, the request URI (without context path) starts with /static/
		String url = StringUtils.removeEnd(target.baseUrl(), "/") + StringUtils.removeStart(request.getRequestURI(), request.getContextPath());
		if (!StringUtils.isEmpty(request.getQueryString())) {
			url = url + "?" + request.getQueryString();
		}

		try {
			method.setURI(URI.create(url));
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		for (String name : Collections.list(request.getHeaderNames())) {
			if (IGNORED_REQUEST_HEADERS.contains(StringUtils.lowerCase(name))) {
				continue;
			}
			for (String value : Collections.list(request.getHeaders(name))) {
				method.addHeader(name, value);
			}
		}

		try (CloseableHttpClient httpClient = RenderUtils.getHttpClient(target.insecure());
				CloseableHttpResponse portalResponse = httpClient.execute(method)) {
			response.setStatus(portalResponse.getStatusLine().getStatusCode());
			for (Header header : portalResponse.getAllHeaders()) {
				if (IGNORED_RESPONSE_HEADERS.contains(StringUtils.lowerCase(header.getName()))) {
					continue;
				}
				response.addHeader(header.getName(), header.getValue());
			}

			HttpEntity entity = portalResponse.getEntity();
			if (entity != null) {
				entity.writeTo(response.getOutputStream());
			}
		} catch (NodeException | IOException e) {
			logger.error(String.format("Error while proxying %s to the portal of node %s", url, target.nodeName()), e);
			if (!response.isCommitted()) {
				response.reset();
				response.sendError(HttpServletResponse.SC_BAD_GATEWAY);
			}
		}
	}

	/**
	 * Get the portal target for the node, which is identified by the cookie. The node is loaded with the permissions of the user of the session.
	 * @param request request
	 * @return portal target
	 * @throws NotProxiedException if the request cannot be proxied
	 */
	protected PortalTarget getPortalTarget(HttpServletRequest request) throws NotProxiedException {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			throw new NotProxiedException(HttpServletResponse.SC_NOT_FOUND);
		}
		Integer nodeId = Stream.of(cookies).filter(cookie -> RenderUtils.PORTAL_PREVIEW_NODE_COOKIE.equals(cookie.getName()))
				.map(cookie -> ObjectTransformer.getInteger(cookie.getValue(), null)).filter(Objects::nonNull).findFirst()
				.orElseThrow(() -> new NotProxiedException(HttpServletResponse.SC_NOT_FOUND));

		Optional<DBSession> optSession;
		try {
			SessionToken token = new SessionToken(request);
			optSession = Trx.supply(() -> DBSession.load(token));
		} catch (InvalidSessionIdException e) {
			throw new NotProxiedException(HttpServletResponse.SC_UNAUTHORIZED);
		} catch (NodeException e) {
			logger.warn("Error while loading the session", e);
			throw new NotProxiedException(HttpServletResponse.SC_UNAUTHORIZED);
		}
		if (optSession.isEmpty()) {
			throw new NotProxiedException(HttpServletResponse.SC_UNAUTHORIZED);
		}

		try (Trx trx = new Trx(optSession.get(), true)) {
			Node node = MiscUtils.load(Node.class, Integer.toString(nodeId), false, ObjectPermission.view);
			if (node == null || StringUtils.isBlank(node.getEffectiveMeshPreviewUrl())) {
				throw new NotProxiedException(HttpServletResponse.SC_NOT_FOUND);
			}
			PortalTarget target = new PortalTarget(node.getFolder().getName(), NodePreviewProxyResourceImpl.getBaseUrl(node),
					node.isInsecurePreviewUrl());
			trx.success();
			return target;
		} catch (InsufficientPrivilegesException e) {
			throw new NotProxiedException(HttpServletResponse.SC_FORBIDDEN);
		} catch (NodeException e) {
			logger.warn(String.format("Cannot proxy static file request for node %d to the portal", nodeId), e);
			throw new NotProxiedException(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
