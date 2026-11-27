package com.gentics.contentnode.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;

import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the metrics served by the {@link OSSRunner}, when metrics are enabled
 */
public class OSSRunnerMetricsTest {
	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * Enable metrics on a random port for the OSSRunner
	 */
	private static ExternalResource metricsEnabled = new ExternalResource() {
		@Override
		protected void before() throws Throwable {
			System.setProperty(ConfigurationValue.METRICS_ENABLED.getSystemPropertyName(), "true");
			System.setProperty(ConfigurationValue.METRICS_PORT.getSystemPropertyName(), "0");
		}

		@Override
		protected void after() {
			System.clearProperty(ConfigurationValue.METRICS_ENABLED.getSystemPropertyName());
			System.clearProperty(ConfigurationValue.METRICS_PORT.getSystemPropertyName());
		}
	};

	/**
	 * OSSRunner context
	 */
	private static OSSRunnerContext ossRunnerContext = new OSSRunnerContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(metricsEnabled).around(ossRunnerContext);

	/**
	 * Test that the metrics are served on the metrics port and contain the CMS specific and the REST API metrics
	 * @throws IOException
	 */
	@Test
	public void testMetricsOnMetricsPort() throws IOException, InterruptedException {
		assertThat(OSSRunner.getMetricsPort()).as("Metrics port").isGreaterThan(0).isNotEqualTo(OSSRunner.getPort());

		// do a REST API request, so that the request metrics are created
		GetMethod rest = new GetMethod(String.format("http://localhost:%d/rest/info/maintenance", OSSRunner.getPort()));
		assertThat(new HttpClient().executeMethod(rest)).as("REST response code").isEqualTo(200);

		// the request metrics are recorded when Jersey finishes the request, which may happen after the response was received,
		// so the metrics are fetched until they contain the request metrics (or the time is up)
		String metrics = getMetrics();
		long end = System.currentTimeMillis() + 10_000;
		while (!metrics.contains("http_server_requests_seconds_count{") && System.currentTimeMillis() < end) {
			Thread.sleep(100);
			metrics = getMetrics();
		}

		assertThat(metrics).as("Metrics").contains("cms_transactions_total", "cms_db_pool_connections", "jvm_memory_used_bytes",
				"http_server_requests_seconds_count{");
	}

	/**
	 * Get the metrics from the metrics port
	 * @return metrics
	 * @throws IOException
	 */
	protected String getMetrics() throws IOException {
		GetMethod get = new GetMethod(String.format("http://localhost:%d/metrics", OSSRunner.getMetricsPort()));
		assertThat(new HttpClient().executeMethod(get)).as("Response code").isEqualTo(200);
		return get.getResponseBodyAsString();
	}

	/**
	 * Test that the metrics are not served on the port of the CMS and the CMS is not served on the metrics port
	 * @throws IOException
	 */
	@Test
	public void testNoMetricsOnCmsPort() throws IOException {
		GetMethod metricsOnCmsPort = new GetMethod(String.format("http://localhost:%d/metrics", OSSRunner.getPort()));
		assertThat(new HttpClient().executeMethod(metricsOnCmsPort)).as("Response code of /metrics on CMS port").isEqualTo(404);

		GetMethod restOnMetricsPort = new GetMethod(String.format("http://localhost:%d/rest/info/maintenance", OSSRunner.getMetricsPort()));
		assertThat(new HttpClient().executeMethod(restOnMetricsPort)).as("Response code of REST API on metrics port").isEqualTo(404);
	}
}
