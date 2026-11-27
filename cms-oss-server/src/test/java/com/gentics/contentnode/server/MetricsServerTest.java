package com.gentics.contentnode.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Test cases for the {@link MetricsServer}
 */
public class MetricsServerTest {
	/**
	 * Registry
	 */
	private static PrometheusMeterRegistry registry;

	/**
	 * Tested server
	 */
	private static MetricsServer metricsServer;

	@BeforeClass
	public static void setupOnce() {
		registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		registry.counter("test.counter").increment(42);

		metricsServer = new MetricsServer("127.0.0.1", 0, registry);
		assertThat(metricsServer.start()).as("Server started").isTrue();
	}

	@AfterClass
	public static void tearDownOnce() {
		metricsServer.stop();
		registry.close();
	}

	/**
	 * Test getting the metrics in the Prometheus text format
	 * @throws IOException
	 */
	@Test
	public void testGetMetrics() throws IOException {
		GetMethod get = new GetMethod(url(MetricsServer.PATH));

		assertThat(new HttpClient().executeMethod(get)).as("Response code").isEqualTo(200);
		assertThat(get.getResponseHeader("Content-Type").getValue()).as("Content-Type").startsWith("text/plain");
		assertThat(get.getResponseBodyAsString()).as("Response body").contains("test_counter_total 42.0");
	}

	/**
	 * Test getting the metrics in the OpenMetrics format
	 * @throws IOException
	 */
	@Test
	public void testGetOpenMetrics() throws IOException {
		GetMethod get = new GetMethod(url(MetricsServer.PATH));
		get.setRequestHeader("Accept", "application/openmetrics-text; version=1.0.0");

		assertThat(new HttpClient().executeMethod(get)).as("Response code").isEqualTo(200);
		assertThat(get.getResponseHeader("Content-Type").getValue()).as("Content-Type").startsWith("application/openmetrics-text");
		assertThat(get.getResponseBodyAsString()).as("Response body").contains("test_counter_total 42.0").contains("# EOF");
	}

	/**
	 * Test that other paths are not found
	 * @throws IOException
	 */
	@Test
	public void testOtherPath() throws IOException {
		assertThat(new HttpClient().executeMethod(new GetMethod(url("/")))).as("Response code").isEqualTo(404);
		assertThat(new HttpClient().executeMethod(new GetMethod(url("/metrics/other")))).as("Response code").isEqualTo(404);
	}

	/**
	 * Test that other methods are not allowed
	 * @throws IOException
	 */
	@Test
	public void testPost() throws IOException {
		assertThat(new HttpClient().executeMethod(new PostMethod(url(MetricsServer.PATH)))).as("Response code").isEqualTo(405);
	}

	/**
	 * Get the URL for the given path on the metrics server
	 * @param path path
	 * @return URL
	 */
	protected String url(String path) {
		return String.format("http://127.0.0.1:%d%s", metricsServer.getPort(), path);
	}
}
