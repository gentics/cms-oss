package com.gentics.contentnode.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.contentnode.factory.DBSession;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.jmx.PublisherInfo;
import com.gentics.contentnode.jmx.PublisherPhase;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

/**
 * Test cases for the {@link CmsMetrics}
 */
public class CmsMetricsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Reader for the metrics forwarded to OpenTelemetry
	 */
	private InMemoryMetricReader metricReader;

	/**
	 * OpenTelemetry meter provider
	 */
	private SdkMeterProvider meterProvider;

	@Before
	public void setup() {
		metricReader = InMemoryMetricReader.create();
		meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
		CmsMetrics.init(OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build());
	}

	@After
	public void tearDown() {
		CmsMetrics.shutdown();
		meterProvider.close();
	}

	/**
	 * Get the registry for reading meter values. This is the Prometheus registry, because reading values from the composite
	 * registry would read from any of its registries and the OpenTelemetry registry does not support reading values (returns NaN).
	 * @return registry
	 */
	protected MeterRegistry readableRegistry() {
		return CmsMetrics.getPrometheusRegistry().orElseThrow();
	}

	/**
	 * Test that only the CMS specific meters are forwarded to OpenTelemetry
	 */
	@Test
	public void testOnlyCmsMetersForwardedToOpenTelemetry() {
		Set<String> names = metricReader.collectAllMetrics().stream().map(MetricData::getName).collect(Collectors.toSet());

		assertThat(names).as("Forwarded metrics").isNotEmpty().allMatch(name -> name.startsWith(CmsMetrics.PREFIX))
				.contains("cms.transactions", "cms.transactions.open", "cms.publish.running", "cms.db.pool.connections",
						"cms.sessions.active", "cms.cache.size", "cms.cache.gets");
	}

	/**
	 * Test that the Prometheus registry contains the JVM meters and the CMS specific meters
	 */
	@Test
	public void testPrometheusContainsAllMeters() {
		String scrape = CmsMetrics.getPrometheusRegistry().orElseThrow().scrape();

		assertThat(scrape).as("Prometheus scrape").contains("jvm_memory_used_bytes", "process_uptime_seconds",
				"cms_transactions_total", "cms_transactions_open ", "cms_transactions_open_max ", "cms_publish_running ",
				"cms_publish_phase{phase=\"idle\"}", "cms_publish_objects{status=\"remaining\",type=\"page\"}",
				"cms_sessions_active ", "cms_db_pool_connections{state=\"active\"}", "cms_db_pool_max_active ",
				"cms_cache_size{region=\"" + NodeFactory.CACHEREGION + "\"}",
				"cms_cache_gets_total{region=\"" + NodeFactory.CACHEREGION + "\",result=\"hit_ram\"}");
	}

	/**
	 * Test that the transaction counter is increased with every transaction
	 * @throws Exception
	 */
	@Test
	public void testTransactions() throws Exception {
		MeterRegistry registry = readableRegistry();
		double before = registry.get("cms.transactions").functionCounter().count();

		Trx.operate(() -> {
			assertThat(registry.get("cms.transactions.open").gauge().value()).as("Open transactions").isGreaterThanOrEqualTo(1);
		});

		assertThat(registry.get("cms.transactions").functionCounter().count()).as("Transactions").isGreaterThan(before);
	}

	/**
	 * Test the meters for the connection pool
	 * @throws Exception
	 */
	@Test
	public void testConnectionPool() throws Exception {
		MeterRegistry registry = readableRegistry();

		// use a pooled connection, so that the pool exists
		Trx.operate(t -> t.getObject(SystemUser.class, 1));

		assertThat(registry.get("cms.db.pool.connections").tag("state", "active").gauge().value()).as("Active connections")
				.isGreaterThanOrEqualTo(0);
		assertThat(registry.get("cms.db.pool.connections").tag("state", "idle").gauge().value()).as("Idle connections")
				.isGreaterThanOrEqualTo(1);
		assertThat(registry.get("cms.db.pool.max.active").gauge().value()).as("Max active connections").isNotNaN();
	}

	/**
	 * Test the meter for active sessions
	 * @throws Exception
	 */
	@Test
	public void testSessions() throws Exception {
		UserGroup nodeGroup = Trx.supply(t -> t.getObject(UserGroup.class, 2));
		SystemUser user = Trx.supply(() -> Creator.createUser("metrics", "metrics", "metrics", "metrics", "", Arrays.asList(nodeGroup)));

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		try {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			SessionMetrics sessionMetrics = new SessionMetrics(executor);
			sessionMetrics.bindTo(registry);

			sessionMetrics.refresh();
			double before = registry.get("cms.sessions.active").gauge().value();
			assertThat(before).as("Active sessions before login").isNotNaN();

			Trx.supply(() -> new DBSession(user, "", "CmsMetricsTest"));
			assertThat(registry.get("cms.sessions.active").gauge().value()).as("Active sessions before refresh").isEqualTo(before);

			sessionMetrics.refresh();
			assertThat(registry.get("cms.sessions.active").gauge().value()).as("Active sessions after refresh").isEqualTo(before + 1);
		} finally {
			executor.shutdownNow();
		}
	}

	/**
	 * Test the meters for the publish process
	 */
	@Test
	public void testPublishProcess() {
		MeterRegistry registry = readableRegistry();
		PublisherInfo info = MBeanRegistry.getPublisherInfo();

		try {
			info.setRunning(true);
			info.setPhase(PublisherPhase.PAGES);
			info.setObjectsToPublish(1, 10, 2, 3, 4);
			info.publishedPage(1);

			assertThat(registry.get("cms.publish.running").gauge().value()).as("Running").isEqualTo(1);
			assertThat(registry.get("cms.publish.phase").tag("phase", "pages").gauge().value()).as("Phase pages").isEqualTo(1);
			assertThat(registry.get("cms.publish.phase").tag("phase", "idle").gauge().value()).as("Phase idle").isEqualTo(0);
			assertThat(registry.get("cms.publish.objects").tags("type", "page", "status", "total").gauge().value()).as("Total pages").isEqualTo(10);
			assertThat(registry.get("cms.publish.objects").tags("type", "page", "status", "published").gauge().value()).as("Published pages").isEqualTo(1);
			assertThat(registry.get("cms.publish.objects").tags("type", "page", "status", "remaining").gauge().value()).as("Remaining pages").isEqualTo(9);
			assertThat(registry.get("cms.publish.objects").tags("type", "form", "status", "total").gauge().value()).as("Total forms").isEqualTo(4);
		} finally {
			info.setRunning(false);
			info.setPhase(PublisherPhase.IDLE);
		}

		assertThat(registry.get("cms.publish.running").gauge().value()).as("Running").isEqualTo(0);
		assertThat(registry.get("cms.publish.objects").tags("type", "page", "status", "total").gauge().value()).as("Total pages").isEqualTo(0);
	}

	/**
	 * Test the meters for the cache regions, including a region, which is created after the meters were bound
	 * @throws Exception
	 */
	@Test
	public void testCacheRegions() throws Exception {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		try {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			CacheMetrics cacheMetrics = new CacheMetrics(executor);
			cacheMetrics.bindTo(registry);

			String region = "gentics-metrics-test";
			assertThat(registry.find("cms.cache.size").tag("region", region).gauge()).as("Meter for new region before rescan").isNull();

			PortalCache cache = PortalCache.getCache(region);
			cache.put("key", "value");
			assertThat(cache.get("key")).isEqualTo("value");
			assertThat(cache.get("inexistent")).isNull();

			cacheMetrics.bindNewRegions();

			assertThat(registry.get("cms.cache.size").tag("region", region).gauge().value()).as("Cache size").isEqualTo(1);
			assertThat(registry.get("cms.cache.gets").tags("region", region, "result", "hit_ram").functionCounter().count()).as("Hits").isEqualTo(1);
			assertThat(registry.get("cms.cache.gets").tags("region", region, "result", "miss_notfound").functionCounter().count()).as("Misses").isEqualTo(1);
		} finally {
			executor.shutdownNow();
		}
	}

	/**
	 * Test that metrics are not initialized when disabled (default) and that shutdown removes the registries
	 */
	@Test
	public void testDisabled() {
		CmsMetrics.shutdown();
		assertThat(CmsMetrics.getRegistry()).as("Registry after shutdown").isEmpty();

		CmsMetrics.init();
		assertThat(CmsMetrics.isEnabled()).as("Metrics enabled").isFalse();
		assertThat(CmsMetrics.getRegistry()).as("Registry when disabled").isEmpty();
		assertThat(CmsMetrics.getPrometheusRegistry()).as("Prometheus registry when disabled").isEmpty();
	}
}
