package com.gentics.contentnode.server;

import static com.gentics.contentnode.runtime.ConfigurationValue.ALOHAEDITOR_PATH;
import static com.gentics.contentnode.runtime.ConfigurationValue.ALOHAEDITOR_PLUGINS_PATH;
import static com.gentics.contentnode.runtime.ConfigurationValue.GCNJSAPI_PATH;
import static com.gentics.contentnode.runtime.ConfigurationValue.HTTP2;
import static com.gentics.contentnode.runtime.ConfigurationValue.HTTP_PORT;
import static com.gentics.contentnode.runtime.ConfigurationValue.STATIC_SERVE_LIST;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ResourceServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.rewrite.handler.RedirectRegexRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.config.AutoScanFeature;
import com.gentics.contentnode.config.PackageRewriteRule;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.init.Initializer;
import com.gentics.contentnode.rest.AcceptResponseServletFilter;
import com.gentics.contentnode.rest.configuration.RESTApplication;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.servlets.AlohaPageServlet;
import com.gentics.contentnode.servlets.GenticsImageStoreServlet;
import com.gentics.contentnode.servlets.JmxServlet;
import com.gentics.contentnode.servlets.SelectedSymlinkAllowedResourceAliasChecker;
import com.gentics.lib.log.NodeLogger;

import jakarta.servlet.DispatcherType;

/**
 * Server Runner for the GCMS
 */
public class OSSRunner {

	static NodeLogger log;

	/**
	 * Server instance
	 */
	protected static Server server;

	/**
	 * Loader for implementations of {@link ServletContextHandlerService}
	 */
	protected static ServiceLoaderUtil<ServletContextHandlerService> servletContextHandlerServiceLoader;

	/**
	 * Main method
	 *
	 * @param args arguments
	 * @throws Exception
	 */
	public static void main(String[] args) {
		start();
	}

	/**
	 * Check whether the server has been started
	 * @return true iff the server has been started
	 */
	public static boolean isServerStarted() {
		return server != null && server.isStarted();
	}

	/**
	 * Check whether the server has not been started or has been stopped
	 * @return true iff the server has been stopped
	 */
	public static boolean isServerStopped() {
		return server == null || server.isStopped();
	}

	/**
	 * Check whether the server start has failed
	 * @return true iff the server start has failed
	 */
	public static boolean isServerFailed() {
		return server != null && server.isFailed();
	}

	/**
	 * Stop the server
	 * @throws Exception
	 */
	public static void stop() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

	/**
	 * Get the port, the server is listening to. If the server has not been started, this return something < 0
	 * @return port (negative, if the server is not started)
	 */
	public static int getPort() {
		if (server == null) {
			return -1;
		} else {
			Connector[] connectors = server.getConnectors();
			if (ArrayUtils.isEmpty(connectors)) {
				return -1;
			} else {
				return Stream.of(connectors).filter(conn -> conn instanceof NetworkConnector)
						.map(conn -> NetworkConnector.class.cast(conn)).map(conn -> conn.getLocalPort()).findFirst()
						.orElse(-1);
			}
		}
	}

	/**
	 * Start the server
	 */
	protected static void start() {
		Initializer.get().init();
		log = NodeLogger.getNodeLogger(OSSRunner.class);
		servletContextHandlerServiceLoader = ServiceLoaderUtil.load(ServletContextHandlerService.class);
		// set the loader also to the NodeConfigRuntimeConfiguration, so that the services can be called when the configuration is reloaded
		NodeConfigRuntimeConfiguration.setServletContextHandlerServiceLoader(servletContextHandlerServiceLoader);

		int port = Integer.parseInt(HTTP_PORT.get());
		boolean http2 = Boolean.parseBoolean(HTTP2.get());

		// create context
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setBaseResource(ResourceFactory.of(context).newClassLoaderResource("/webroot", false));

		// add REST API Servlet
		ResourceConfig resourceConfig = ResourceConfig.forApplication(new RESTApplication());
		resourceConfig.register(AutoScanFeature.class);

		ServletContainer container = new ServletContainer(resourceConfig);
		ServletHolder servletHolder = new ServletHolder(container);
		context.addServlet(servletHolder, "/rest/*");
		context.addServlet(JmxServlet.class, "/jmx");

		// add servlets for alohaeditor
		addAlohaEditor(context);

		// add UIs
		context.addServlet(getWebrootResourceServlet("editor"), "/editor/*");
		context.addServlet(getWebrootResourceServlet("admin"), "/admin/*");
		context.addServlet(
				getStaticFileServletForPath(ConfigurationValue.UI_CONF_PATH.get()),
				"/ui-conf/*");

		// add tools
		context.addServlet(getWebrootResourceServlet("tools"), "/tools/*");

		// add openapi
		context.addServlet(getWebrootResourceServlet("openapi"), "/openapi/*");

		// add other static files
		addStaticFilesToContext(context);

		// let implementations of ServletContextHandlerService modify the context
		servletContextHandlerServiceLoader.forEach(service -> service.init(context));

		// add AcceptResponseFilter
		addAcceptResponseServletFilter(context);

		// add GenticsImageStoreServlet
		context.addServlet(GenticsImageStoreServlet.class, "/GenticsImageStore/*");

		context.addServlet(getStaticFileServletForPath(ConfigurationValue.PACKAGES_PATH.get()),	"/packages/*");

		var rewriteHandler = createRewriteHandler();
		rewriteHandler.setHandler(context);

		// set error pages
		context.addServlet(getWebrootResourceServlet("error"), "/error/*");
		ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
		errorHandler.addErrorPage(HttpStatus.FORBIDDEN_403, "/error/403.html");
		errorHandler.addErrorPage(HttpStatus.NOT_FOUND_404, "/error/404.html");
		errorHandler.addErrorPage(HttpStatus.INTERNAL_SERVER_ERROR_500, "/error/500.html");
		context.setErrorHandler(errorHandler);

		// set the connection factory up
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);
		// Allow symlinks in the resource accessor. Keep in sync with the ones in createRewriteHandler().
		context.addAliasCheck(
				new SelectedSymlinkAllowedResourceAliasChecker(
						context, 
						Set.of("(.*)\\/packages\\/([^\\/]*)\\/files\\/(.*)", "(.*)\\/packages\\/([^\\/]*)\\/files-internal\\/(.*)")));

		// create server
		server = new Server();
		server.setStopAtShutdown(true);
		server.setHandler(rewriteHandler);

		// create connector
		ServerConnector serverConnector;
		if (http2) {
			HTTP2CServerConnectionFactory h2cConnectionFactory = new HTTP2CServerConnectionFactory(httpConfiguration);
			serverConnector = new ServerConnector(server, httpConnectionFactory, h2cConnectionFactory);
		} else {
			serverConnector = new ServerConnector(server, httpConnectionFactory);
		}
		serverConnector.setPort(port);
		server.addConnector(serverConnector);

		// add the ForwardedRequestCustomizer in order to support the CMS running behind a proxy, which e.g. terminates SSL
		for (Connector c : server.getConnectors()) {
			c.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().addCustomizer(new ForwardedRequestCustomizer());
		}

		Slf4jRequestLogWriter logWriter = new Slf4jRequestLogWriter();
		logWriter.setLoggerName("access_log");
		server.setRequestLog(new CustomRequestLog(logWriter, ConfigurationValue.ACCESS_LOG.get()));

		try {
			NodeConfigRuntimeConfiguration.runtimeLog.info(String.format("Starting server at port %d", port));

			// start server
			server.start();

			NodeConfigRuntimeConfiguration.runtimeLog.info("Server started successfully");
			Thread.currentThread().join();
		} catch (Exception e) {
			NodeConfigRuntimeConfiguration.runtimeLog.error("Server startup failed", e);
			try {
				server.stop();
			} catch (Exception ignored) {
			}
		} finally {
			Initializer.get().shutdown();
		}
	}

	private static void addStaticFilesToContext(ServletContextHandler context) {
		var resources = STATIC_SERVE_LIST.get();
		var folderServeList = List.of(resources.split(","));

		folderServeList.forEach(servePath -> {
			var path = Paths.get(servePath);

			context.addServlet(
					getStaticFileServletForPath(path.toString()),
					String.format("/%s/*", path.getFileName().toString())
			);
		});
	}

	/**
	 * Creates a RewriteHandler that handles all url rewrites.
	 * That means all rules are added to this handler.
	 * Example: We want to have a rule that the Editor-UI will be available at /
	 * @return The RewriteHandler with all rules
	 */
	private static RewriteHandler createRewriteHandler() {
		var rewriteHandler = new RewriteHandler();

		var rewriteStaticRule = new PackageRewriteRule("^/static/([^/]*)/files/(.*)",
				"/packages/$1/files/$2");

		var rewriteInternalRule = new PackageRewriteRule("^/internal/([^/]*)/files/(.*)",
				"/packages/$1/files-internal/$2");

		rewriteHandler.addRule(rewriteStaticRule);
		rewriteHandler.addRule(rewriteInternalRule);

		RedirectRegexRule redirectToEditor = new RedirectRegexRule("^/$", "/editor");
		rewriteHandler.addRule(redirectToEditor);

		return rewriteHandler;
	}

	/**
	 * Get the holder for a {@link ResourceServlet} with the baseResource set to the given
	 * relative path. Directory listing is forbidden.
	 * @param path path relative to /webroot of the classpath
	 * @return servlet holder instance
	 */
	private static ServletHolder getWebrootResourceServlet(String path) {
		return getResourceServlet(Map.of(
				"baseResource", path,
				"pahtInfoOnly", "true",
				"dirAllowed", "false"));
	}

	/**
	 * Get the holder for a {@link ResourceServlet} with the baseResource set to the given path,
	 * which is made absolute. Directory listing is forbidden, etags will be generated and cacheControl is set to no-cache
	 * @param path path
	 * @return servlet holder instance
	 */
	private static ServletHolder getStaticFileServletForPath(String path) {
		path = Path.of(path).toAbsolutePath().toString();
		return getResourceServlet(Map.of(
				"baseResource", path,
				"pathInfoOnly", "true",
				"dirAllowed", "false",
				"etags", "true",
				"cacheControl", "no-cache"));
	}

	/**
	 * Get the holder for a {@link ResourceServlet} configured with the given init parameters
	 * @param initParameters map of init parameters
	 * @return servlet holder instance
	 */
	private static ServletHolder getResourceServlet(Map<String, String> initParameters) {
		var servletHolder = new ServletHolder(ResourceServlet.class);
		servletHolder.setInitParameters(initParameters);
		return servletHolder;
	}

	/**
	 * Add servlet to either serve alohaeditor from classpath, or from the filesystem (for
	 * development)
	 *
	 * @param context
	 */
	private static void addAlohaEditor(ServletContextHandler context) {
		String alohaEditorPath = ALOHAEDITOR_PATH.get();

		// add AlohaPageServlet
		context.addServlet(AlohaPageServlet.class, "/alohapage");

		// serve AlohaEditor files
		if (StringUtils.isBlank(alohaEditorPath)) {
			// determine the build timestamp
			try (InputStream buildFile = OSSRunner.class.getResourceAsStream("/build.txt")) {
				if (buildFile != null) {
					String buildTimestamp = IOUtils.toString(buildFile);
					if (!StringUtils.startsWith(buildTimestamp, "$")) {
						System.setProperty(AlohaRenderer.BUILD_TIMESTAMP, buildTimestamp);
						System.setProperty(AlohaRenderer.ALOHA_EDITOR_BASE_URL_PARAM,
								String.format("/alohaeditor/%s", buildTimestamp));
					}
				}
			} catch (IOException e) {
				NodeLogger.getNodeLogger(OSSRunner.class)
						.error("Error while reading build.txt from classpath", e);
			}

			// serve alohaeditor files
			context.addServlet(getWebrootResourceServlet("alohaeditor"), "/alohaeditor/*");
		} else {
			System.setProperty(AlohaRenderer.BUILD_TIMESTAMP, "DEV");
			System.setProperty(AlohaRenderer.ALOHA_EDITOR_BASE_URL_PARAM,
					String.format("/alohaeditor/%s", "DEV"));

			String alohaEditorPluginsPath = ALOHAEDITOR_PLUGINS_PATH.get();
			if (!StringUtils.isBlank(alohaEditorPluginsPath)) {
				context.addServlet(getStaticFileServletForPath(alohaEditorPluginsPath),
						"/alohaeditor/DEV/plugins/gcn/*");
			}

			context.addServlet(ResourceServlet.class, "/alohaeditor/gcmsui-scripts-launcher.js");
			context.addServlet(getStaticFileServletForPath(alohaEditorPath), "/alohaeditor/DEV/*");
		}

		String gcnJsApiPath = GCNJSAPI_PATH.get();
		if (StringUtils.isBlank(gcnJsApiPath)) {
			// serve gcnjsapi files
			context.addServlet(getWebrootResourceServlet("gcnjsapi"), "/gcnjsapi/*");
		} else {
			context.addServlet(getStaticFileServletForPath(gcnJsApiPath),
					String.format("/gcnjsapi/%s/*", System.getProperty(AlohaRenderer.BUILD_TIMESTAMP)));
		}

		// add servlet for static images
		context.addServlet(getWebrootResourceServlet("images"), "/images/*");
	}

	/**
	 * Add the AcceptResponseServletFilter
	 *
	 * @param context context
	 */
	protected static void addAcceptResponseServletFilter(ServletContextHandler context) {
		FilterHolder acceptResponseServletFilterHolder = context.addFilter(
				AcceptResponseServletFilter.class,
				"/rest/*", EnumSet.of(DispatcherType.REQUEST));
		acceptResponseServletFilterHolder.setAsyncSupported(true);
		acceptResponseServletFilterHolder.setInitParameter("xml", "application/xml");
		acceptResponseServletFilterHolder.setInitParameter("json", "application/json");
	}
}
