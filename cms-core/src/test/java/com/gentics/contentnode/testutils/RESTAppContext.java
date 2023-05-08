package com.gentics.contentnode.testutils;

import java.io.File;
import java.net.BindException;
import java.net.SocketException;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.rules.ExternalResource;

import com.gentics.contentnode.rest.client.RestClient;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.configuration.RESTApplication;
import com.gentics.lib.log.NodeLogger;

/**
 * REST Application Context. This must always be used in conjunction with a {@link DBTestContext}
 *
 * Usage:
 * <ol>
 *   <li>In {@link #before()} a new HttpServer instance will be started, serving the REST API (If port is already bound, another port will be tried)</li>
 *   <li>Method {@link #getBaseUri()} returns the Base URI for requests to the HttpServer instance</li>
 *   <li>In {@link #after()}, the HttpServer instance will be stopped</li>
 * </ol>
 */
public class RESTAppContext extends ExternalResource {
	/**
	 * Keystore containing the (self-signed) server certificate. Created with
	 * <code>keytool -genkey -alias serverkey -keyalg RSA -keystore grizzly_server.jks</code>
	 */
	private static final String GRIZZLY_SERVER_KEYSTORE = "grizzly_server.jks";

	/**
	 * Password for the keystore
	 */
	private static final String GRIZZLY_SERVER_KEYSTORE_PASS = "123456";

	/**
	 * Logger
	 */
	protected static final NodeLogger logger = NodeLogger.getNodeLogger(RESTAppContext.class);

	/**
	 * Default Base URI Pattern
	 */
	private final static String DEFAULT_BASE_URI_PATTERN = "http://localhost:%d/rest/";

	/**
	 * First port to try (inclusive)
	 */
	private final static int DEFAULT_START_PORT = 8080;

	/**
	 * Port Range
	 */
	private final static int DEFAULT_PORT_RANGE = 100;

	/**
	 * Base URI pattern
	 */
	private String baseUriPattern = DEFAULT_BASE_URI_PATTERN;

	/**
	 * Start port
	 */
	private int startPort = DEFAULT_START_PORT;

	/**
	 * Port range
	 */
	private int portRange = DEFAULT_PORT_RANGE;

	/**
	 * Base URI (including the port)
	 */
	private String baseUri;

	/**
	 * Server port
	 */
	private int port;

	/**
	 * Server type
	 */
	private Type type;

	/**
	 * Grizzly HttpServer instance
	 */
	private HttpServer server;

	/**
	 * Jetty Server instance
	 */
	private Server jettyServer;

	/**
	 * Resource config
	 */
	private ResourceConfig resourceConfig;

	/**
	 * Create context for the default application and {@link Type#jetty}
	 */
	public RESTAppContext() {
		this(Type.jetty, ResourceConfig.forApplication(new RESTApplication()));
	}

	/**
	 * Create context for the given application
	 * @param application application
	 */
	public RESTAppContext(Application application) {
		this(Type.jetty, ResourceConfig.forApplication(application));
	}

	/**
	 * Create context for the given resource config
	 * @param resourceConfig resource config
	 */
	public RESTAppContext(ResourceConfig resourceConfig) {
		this(Type.jetty, resourceConfig);
	}

	/**
	 * Create context for the default application
	 * @param type server type
	 */
	public RESTAppContext(Type type) {
		this(type, ResourceConfig.forApplication(new RESTApplication() {
			@Override
			public Map<String, Object> getProperties() {
				Map<String, Object> properties = super.getProperties();
				properties.put(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, "false");
				return properties;
			}
		}));
	}

	/**
	 * Create context for the given resource config
	 * @param type server type
	 * @param resourceConfig resource config
	 */
	public RESTAppContext(Type type, ResourceConfig resourceConfig) {
		this.type = type;
		this.resourceConfig = resourceConfig;
	}

	/**
	 * Set the base URI Pattern (must contain Placeholder %d for the port)
	 * @param baseUriPattern base URI Pattern
	 * @return fluent API
	 */
	public RESTAppContext baseUriPattern(String baseUriPattern) {
		this.baseUriPattern = baseUriPattern;
		return this;
	}

	/**
	 * Set the start port
	 * @param startPort start port
	 * @return fluent API
	 */
	public RESTAppContext startPort(int startPort) {
		this.startPort = startPort;
		return this;
	}

	/**
	 * Set the port range
	 * @param portRange port range
	 * @return fluent API
	 */
	public RESTAppContext portRange(int portRange) {
		this.portRange = portRange;
		return this;
	}

	@Override
	protected void before() throws Throwable {
		SSLEngineConfigurator sslEngineConfigurator = null;

		switch (type) {
		case jetty:
			ServletContainer container = new ServletContainer(resourceConfig);
			ServletHolder servletHolder = new ServletHolder(container);
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			String path = URI.create(String.format(baseUriPattern, 0)).getPath();
			path = StringUtils.appendIfMissing(path, "/");
			context.addServlet(servletHolder, path + "*");

			jettyServer = new Server(0);
			jettyServer.setHandler(context);

			jettyServer.start();
			port = ((NetworkConnector)jettyServer.getConnectors()[0]).getLocalPort();
			baseUri = String.format(baseUriPattern, port);
			logger.info("HttpServer started");
			break;
		case grizzlySsl:
			SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();

			sslContextConfigurator.setKeyStoreFile(new File(RESTAppContext.class.getResource(GRIZZLY_SERVER_KEYSTORE).toURI()).getAbsolutePath());
			sslContextConfigurator.setKeyStorePass(GRIZZLY_SERVER_KEYSTORE_PASS);

			sslEngineConfigurator = new SSLEngineConfigurator(sslContextConfigurator)
				.setClientMode(false);
			// Intentional fall-through
		case grizzly:
		default:
			port = startPort;
			int endPort = startPort + portRange;
			boolean tryAgain = false;

			do {
				tryAgain = false;
				baseUri = String.format(baseUriPattern, port);

				logger.info(String.format("Starting HttpServer instance with base Uri %s", baseUri));

				try {
					server = sslEngineConfigurator == null
						? GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), resourceConfig)
						: GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), resourceConfig, true, sslEngineConfigurator);
					logger.info(String.format("HTTP%s Server started", sslEngineConfigurator == null ? "" : "S"));
				} catch (Throwable t) {
					if (t instanceof BindException || t.getCause() instanceof BindException || t instanceof SocketException || t.getCause() instanceof SocketException) {
						if (++port < endPort) {
							tryAgain = true;
							logger.info("Could not start HttpServer due to BindException, trying a different port.");
						} else {
							logger.error("Too many BindExceptions, giving up.");
							throw t;
						}
					} else {
						throw t;
					}
				}
			} while (tryAgain);
		}
	}

	@Override
	protected void after() {
		if (server != null) {
			logger.info("Stopping HttpServer instance");
			server.shutdown();
			logger.info("HttpServer stopped");
		}
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

	/**
	 * Create and return an {@link AutoCloseable} instance containing a logged in client
	 * @param login login
	 * @param password password
	 * @return instance
	 * @throws RestException
	 */
	public LoggedInClient client(String login, String password) throws RestException {
		return new LoggedInClient(login, password);
	}

	/**
	 * AutoCloseable implementation that creates a logged in client
	 */
	public class LoggedInClient implements AutoCloseable {
		protected RestClient client;

		/**
		 * Create an instance
		 * @param login login
		 * @param password password
		 * @throws RestException
		 */
		public LoggedInClient(String login, String password) throws RestException {
			client = new RestClient(getBaseUri());
			client.login(login, password);
		}

		@Override
		public void close() throws RestException {
			client.logout();
		}

		/**
		 * Get the client
		 * @return client
		 */
		public RestClient get() {
			return client;
		}
	}

	/**
	 * Server type
	 */
	public enum Type {
		/**
		 * Grizzly HTTP Server (no Servlet specific functionality like cookies)
		 */
		grizzly,

		grizzlySsl,

		/**
		 * Jersey Application Server
		 */
		jetty
}
}
