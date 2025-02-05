package com.gentics.node.testutils;

import jakarta.servlet.Servlet;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.junit.rules.ExternalResource;

import com.gentics.lib.log.NodeLogger;

/**
 * Test Rule for creating a servlet app
 */
public class ServletAppContext extends ExternalResource {
	/**
	 * Logger
	 */
	protected static final NodeLogger logger = NodeLogger.getNodeLogger(ServletAppContext.class);

	/**
	 * Default Base URI Pattern
	 */
	private final static String DEFAULT_BASE_URI_PATTERN = "http://localhost:%d/";

	/**
	 * Base URI pattern
	 */
	private String baseUriPattern = DEFAULT_BASE_URI_PATTERN;

	/**
	 * Base URI (including the port)
	 */
	private String baseUri;

	/**
	 * Server port
	 */
	private int port;

	/**
	 * Jetty Server instance
	 */
	private Server jettyServer;

	private ServletContextHandler handler;

	/**
	 * Create context
	 */
	public ServletAppContext() {
		handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
	}

	/**
	 * Set the base URI Pattern (must contain Placeholder %d for the port)
	 * @param baseUriPattern base URI Pattern
	 * @return fluent API
	 */
	public ServletAppContext baseUriPattern(String baseUriPattern) {
		this.baseUriPattern = baseUriPattern;
		return this;
	}

	/**
	 * Add a servlet at the specified context path (should begin with / but not end with /)
	 * @param path context path
	 * @param servletClass servlet class
	 * @param params optional init parameters
	 * @return fluent API
	 */
	@SuppressWarnings("unchecked")
	public ServletAppContext servlet(String path, Class<? extends Servlet> servletClass,
			Pair<String, String>... params) {
		ServletHolder servletHolder = new ServletHolder(servletClass);
		servletHolder.setInitOrder(0);
		for (Pair<String, String> param : params) {
			servletHolder.setInitParameter(param.getLeft(), param.getRight());
		}
		handler.addServlet(servletHolder, path);
		return this;
	}

	@Override
	protected void before() throws Throwable {
		jettyServer = new Server(0);
		jettyServer.setHandler(handler);

		jettyServer.start();
		port = ((NetworkConnector)jettyServer.getConnectors()[0]).getLocalPort();
		baseUri = String.format(baseUriPattern, port);
		logger.info("HttpServer started");
	}

	@Override
	protected void after() {
		if (jettyServer != null) {
			logger.info("Stopping Jetty");
			try {
				jettyServer.stop();
				logger.info("Jetty stopped");
			} catch (Exception e) {
				logger.error("Error while stopping jetty", e);
			}
		}
	}

	/**
	 * Get the base URI for the REST App
	 * @return base URI
	 */
	public String getBaseUri() {
		return baseUri;
	}

	/**
	 * Get the server port
	 * @return server port
	 */
	public int getPort() {
		return port;
	}
}
