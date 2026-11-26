package com.gentics.contentnode.metrics;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.gentics.contentnode.etc.PrefixedThreadFactory;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.log.NodeLogger;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;

/**
 * Bootstrap for the metrics of the CMS.
 *
 * <p>
 * When {@link ConfigurationValue#METRICS_ENABLED enabled}, the metrics are collected in a {@link CompositeMeterRegistry},
 * which consists of
 * </p>
 * <ul>
 * <li>a {@link PrometheusMeterRegistry}, which contains all meters. Its content is served by the metrics server (see
 * {@code com.gentics.contentnode.server.MetricsServer}) on a separate port.</li>
 * <li>an {@link OpenTelemetryMeterRegistry}, which forwards the meters passing the {@link #OTLP_FILTER} to the
 * {@link GlobalOpenTelemetry} instance. The instance is set by the OpenTelemetry Java agent, which sends the metrics to
 * an OTLP endpoint. Without the agent, the instance is a no-op.</li>
 * </ul>
 */
public final class CmsMetrics {
	/**
	 * Prefix of the names of the CMS specific meters
	 */
	public final static String PREFIX = "cms.";

	/**
	 * Filter for the meters, which are forwarded to OpenTelemetry. Currently only the CMS specific meters are forwarded.
	 */
	final static MeterFilter OTLP_FILTER = MeterFilter.denyUnless(id -> id.getName().startsWith(PREFIX));

	/**
	 * Interval for refreshing values, which are expensive to get (see {@link SessionMetrics}) and for detecting new cache
	 * regions (see {@link CacheMetrics})
	 */
	final static Duration REFRESH_INTERVAL = Duration.ofSeconds(60);

	/**
	 * Logger
	 */
	private final static NodeLogger logger = NodeLogger.getNodeLogger(CmsMetrics.class);

	/**
	 * Registry containing all meters, if initialized
	 */
	private static CompositeMeterRegistry registry;

	/**
	 * Prometheus registry, if initialized
	 */
	private static PrometheusMeterRegistry prometheusRegistry;

	/**
	 * OpenTelemetry registry, if initialized
	 */
	private static MeterRegistry otlpRegistry;

	/**
	 * Executor for the refresh tasks, if initialized
	 */
	private static ScheduledExecutorService executor;

	/**
	 * Binder of the GC metrics, which needs to be closed
	 */
	private static JvmGcMetrics jvmGcMetrics;

	/**
	 * Static class, no instances
	 */
	private CmsMetrics() {
	}

	/**
	 * Check whether metrics are enabled
	 * @return true iff metrics are enabled
	 */
	public static boolean isEnabled() {
		return Boolean.parseBoolean(ConfigurationValue.METRICS_ENABLED.get());
	}

	/**
	 * Initialize the metrics, if {@link #isEnabled() enabled}. Must be called after the CMS (including the caches) has
	 * been initialized. Does nothing, if the metrics are already initialized.
	 */
	public static void init() {
		if (!isEnabled()) {
			logger.info("Metrics are disabled");
			return;
		}
		init(GlobalOpenTelemetry.getOrNoop());
	}

	/**
	 * Initialize the metrics, forwarding the CMS specific meters to the given OpenTelemetry instance.
	 * Does nothing, if the metrics are already initialized.
	 * @param openTelemetry OpenTelemetry instance
	 */
	static synchronized void init(OpenTelemetry openTelemetry) {
		if (registry != null) {
			return;
		}

		prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		otlpRegistry = OpenTelemetryMeterRegistry.builder(openTelemetry).build();
		otlpRegistry.config().meterFilter(OTLP_FILTER);
		registry = new CompositeMeterRegistry(Clock.SYSTEM, List.of(prometheusRegistry, otlpRegistry));

		executor = Executors.newSingleThreadScheduledExecutor(new PrefixedThreadFactory("metrics"));
		jvmGcMetrics = new JvmGcMetrics();

		List<MeterBinder> binders = List.of(new ClassLoaderMetrics(), new JvmMemoryMetrics(), jvmGcMetrics,
				new JvmThreadMetrics(), new ProcessorMetrics(), new UptimeMetrics(), new PublishMetrics(),
				new TransactionMetrics(), new SessionMetrics(executor), new ConnectionPoolMetrics(),
				new CacheMetrics(executor));
		binders.forEach(binder -> binder.bindTo(registry));

		logger.info("Metrics initialized");
	}

	/**
	 * Get the registry containing all meters. Additional meters must be registered here.
	 * Values of meters must not be read from this registry, because the OpenTelemetry registry does not support reading
	 * values (returns NaN) and the composite registry may read from it. Use {@link #getPrometheusRegistry()} for reading values.
	 * @return optional registry, empty if the metrics are not initialized
	 */
	public static synchronized Optional<MeterRegistry> getRegistry() {
		return Optional.ofNullable(registry);
	}

	/**
	 * Get the Prometheus registry
	 * @return optional Prometheus registry, empty if the metrics are not initialized
	 */
	public static synchronized Optional<PrometheusMeterRegistry> getPrometheusRegistry() {
		return Optional.ofNullable(prometheusRegistry);
	}

	/**
	 * Stop collecting metrics and close the registries. Must be called before the CMS is shut down, because the refresh
	 * tasks access the database and the caches.
	 */
	public static synchronized void shutdown() {
		if (registry == null) {
			return;
		}

		executor.shutdownNow();
		try {
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				logger.warn("Metrics refresh tasks did not terminate");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		jvmGcMetrics.close();
		registry.close();
		prometheusRegistry.close();
		otlpRegistry.close();

		registry = null;
		prometheusRegistry = null;
		otlpRegistry = null;
		executor = null;
		jvmGcMetrics = null;

		logger.info("Metrics shut down");
	}
}
