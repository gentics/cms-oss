package com.gentics.contentnode.server;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.gentics.lib.log.NodeLogger;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.expositionformats.ExpositionFormatWriter;
import io.prometheus.metrics.expositionformats.ExpositionFormats;

/**
 * Server, which serves the metrics in the Prometheus format at {@link #PATH} on a separate port.
 *
 * <p>
 * The server does not use the {@link org.eclipse.jetty.server.ForwardedRequestCustomizer} and should only be
 * reachable from the monitoring system, since the metrics are not protected otherwise.
 * </p>
 */
public class MetricsServer {
	/**
	 * Path of the metrics endpoint
	 */
	public final static String PATH = "/metrics";

	/**
	 * Logger
	 */
	private final static NodeLogger logger = NodeLogger.getNodeLogger(MetricsServer.class);

	/**
	 * Server instance
	 */
	private final Server server;

	/**
	 * Connector
	 */
	private final ServerConnector connector;

	/**
	 * Create an instance
	 * @param host host (interface) to bind to
	 * @param port port to listen to (0 for a random port)
	 * @param registry registry containing the metrics
	 */
	public MetricsServer(String host, int port, PrometheusMeterRegistry registry) {
		QueuedThreadPool threadPool = new QueuedThreadPool(8, 1);
		threadPool.setName("metrics-server");

		server = new Server(threadPool);
		server.setStopAtShutdown(true);

		connector = new ServerConnector(server, 1, 1);
		connector.setHost(host);
		connector.setPort(port);
		server.addConnector(connector);

		server.setHandler(new MetricsHandler(registry));
	}

	/**
	 * Start the server. If starting fails (e.g. because the port is already in use), the error is logged, but not thrown,
	 * because the CMS can be used without the metrics.
	 * @return true iff the server was started
	 */
	public boolean start() {
		try {
			server.start();
			logger.info(String.format("Serving metrics at %s:%d%s", connector.getHost(), getPort(), PATH));
			return true;
		} catch (Exception e) {
			logger.error(String.format("Could not start metrics server at %s:%d", connector.getHost(), connector.getPort()), e);
			try {
				server.stop();
			} catch (Exception ignored) {
			}
			return false;
		}
	}

	/**
	 * Stop the server
	 */
	public void stop() {
		try {
			server.stop();
		} catch (Exception e) {
			logger.error("Error while stopping metrics server", e);
		}
	}

	/**
	 * Get the port, the server is listening to
	 * @return port (negative, if the server is not started)
	 */
	public int getPort() {
		return connector.getLocalPort();
	}

	/**
	 * Handler serving the metrics
	 */
	protected static class MetricsHandler extends Handler.Abstract {
		/**
		 * Registry
		 */
		private final PrometheusMeterRegistry registry;

		/**
		 * Supported exposition formats, the format is selected by the Accept header of the request
		 */
		private final ExpositionFormats expositionFormats = ExpositionFormats.init();

		/**
		 * Create an instance
		 * @param registry registry
		 */
		public MetricsHandler(PrometheusMeterRegistry registry) {
			this.registry = registry;
		}

		@Override
		public boolean handle(Request request, Response response, Callback callback) throws Exception {
			if (!PATH.equals(Request.getPathInContext(request))) {
				Response.writeError(request, response, callback, HttpStatus.NOT_FOUND_404);
				return true;
			}
			if (!HttpMethod.GET.is(request.getMethod())) {
				Response.writeError(request, response, callback, HttpStatus.METHOD_NOT_ALLOWED_405);
				return true;
			}

			ExpositionFormatWriter writer = expositionFormats.findWriter(request.getHeaders().get(HttpHeader.ACCEPT));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			writer.write(out, registry.getPrometheusRegistry().scrape());

			response.setStatus(HttpStatus.OK_200);
			response.getHeaders().put(HttpHeader.CONTENT_TYPE, writer.getContentType());
			response.write(true, ByteBuffer.wrap(out.toByteArray()), callback);
			return true;
		}
	}
}
