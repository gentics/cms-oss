package com.gentics.contentnode.server;

import static com.gentics.contentnode.runtime.ConfigurationValue.ALOHAEDITOR_PATH;
import static com.gentics.contentnode.runtime.ConfigurationValue.ALOHAEDITOR_PLUGINS_PATH;
import static com.gentics.contentnode.runtime.ConfigurationValue.GCNJSAPI_PATH;
import static com.gentics.contentnode.runtime.ConfigurationValue.HTTP_PORT;
import static com.gentics.contentnode.runtime.ConfigurationValue.STATIC_SERVE_LIST;
import static com.gentics.contentnode.runtime.ConfigurationValue.HTTP2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.ServiceLoader;

import javax.servlet.DispatcherType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.rewrite.handler.RedirectRegexRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.config.AutoScanFeature;
import com.gentics.contentnode.config.PackageRewriteRule;
import com.gentics.contentnode.init.Initializer;
import com.gentics.contentnode.rest.AcceptResponseServletFilter;
import com.gentics.contentnode.rest.configuration.RESTApplication;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.servlets.AlohaPageServlet;
import com.gentics.contentnode.servlets.GenticsImageStoreServlet;
import com.gentics.contentnode.servlets.JmxServlet;
import com.gentics.lib.log.NodeLogger;

/**
 * Server Runner for the GCMS
 */
public class OSSRunner {

	static NodeLogger log;

	/**
	 * Loader for implementations of {@link ServletContextHandlerService}
	 */
	protected static ServiceLoader<ServletContextHandlerService> servletContextHandlerServiceLoader;

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
	 * Start the server
	 */
	protected static void start() {
		Initializer.get().init();
		log = NodeLogger.getNodeLogger(OSSRunner.class);
		servletContextHandlerServiceLoader = ServiceLoader.load(ServletContextHandlerService.class);
		// set the loader also to the NodeConfigRuntimeConfiguration, so that the services can be called when the configuration is reloaded
		NodeConfigRuntimeConfiguration.setServletContextHandlerServiceLoader(servletContextHandlerServiceLoader);

		int port = Integer.parseInt(HTTP_PORT.get());
		boolean useHttp2 = Boolean.parseBoolean(HTTP2.get());

		// create context
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setBaseResource(Resource.newClassPathResource("/webroot"));

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
		context.addServlet(DefaultServlet.class, "/editor/*");
		context.addServlet(DefaultServlet.class, "/admin/*");
		context.addServlet(
				getStaticFileServletForPath(ConfigurationValue.UI_CONF_PATH.get()),
				"/ui-conf/*");

		// add tools
		context.addServlet(DefaultServlet.class, "/tools/*").setInitParameter("dirAllowed", "false");

		// add openapi
		context.addServlet(DefaultServlet.class, "/openapi/*").setInitParameter("dirAllowed", "false");

		// add other static files
		addStaticFilesToContext(context);

		// let implementations of ServletContextHandlerService modify the context
		servletContextHandlerServiceLoader.forEach(service -> service.init(context));

		// add AcceptResponseFilter
		addAcceptResponseServletFilter(context);

		// add GenticsImageStoreServlet
		context.addServlet(GenticsImageStoreServlet.class, "/GenticsImageStore/*");

		context.addServlet(getStaticFileServletForPath(ConfigurationValue.PACKAGES_PATH.get()),
				"/packages/*");

		var rewriteHandler = createRewriteHandler();
		rewriteHandler.setHandler(context);

		// set error pages
		context.addServlet(DefaultServlet.class, "/error/*").setInitParameter("dirAllowed", "false");
		ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
		errorHandler.addErrorPage(HttpStatus.FORBIDDEN_403, "/error/403.html");
		errorHandler.addErrorPage(HttpStatus.NOT_FOUND_404, "/error/404.html");
		errorHandler.addErrorPage(HttpStatus.INTERNAL_SERVER_ERROR_500, "/error/500.html");
		context.setErrorHandler(errorHandler);

		// set the connection factory up
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);

		// create server
		Server server = new Server();
		server.setStopAtShutdown(true);
		server.setHandler(rewriteHandler);

		// create connector
		ServerConnector serverConnector;
		if (useHttp2) {
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
			e.printStackTrace();
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

	private static ServletHolder getStaticFileServletForPath(String path) {
		var defaultServlet = new DefaultServlet();
		var holderServlet = new ServletHolder("default_" + path, defaultServlet);
		holderServlet.setInitParameter("resourceBase", path);
		// only apply path info to resourceBase
		holderServlet.setInitParameter("pathInfoOnly", "true");
		holderServlet.setInitParameter("dirAllowed", "false");

		return holderServlet;
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
			context.addServlet(DefaultServlet.class, "/alohaeditor/*").setInitParameter("dirAllowed", "false");
		} else {
			System.setProperty(AlohaRenderer.BUILD_TIMESTAMP, "DEV");
			System.setProperty(AlohaRenderer.ALOHA_EDITOR_BASE_URL_PARAM,
					String.format("/alohaeditor/%s", "DEV"));

			String alohaEditorPluginsPath = ALOHAEDITOR_PLUGINS_PATH.get();
			if (!StringUtils.isBlank(alohaEditorPluginsPath)) {
				context.addServlet(getStaticFileServletForPath(alohaEditorPluginsPath),
						"/alohaeditor/DEV/plugins/gcn/*");
			}

			context.addServlet(DefaultServlet.class, "/alohaeditor/gcmsui-scripts-launcher.js");
			context.addServlet(getStaticFileServletForPath(alohaEditorPath), "/alohaeditor/DEV/*");
		}

		String gcnJsApiPath = GCNJSAPI_PATH.get();
		if (StringUtils.isBlank(gcnJsApiPath)) {
			// serve gcnjsapi files
			context.addServlet(DefaultServlet.class, "/gcnjsapi/*").setInitParameter("dirAllowed", "false");
		} else {
			context.addServlet(getStaticFileServletForPath(gcnJsApiPath),
					String.format("/gcnjsapi/%s/*", System.getProperty(AlohaRenderer.BUILD_TIMESTAMP)));
		}

		// add servlet for static images
		context.addServlet(DefaultServlet.class, "/images/*").setInitParameter("dirAllowed", "false");
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
