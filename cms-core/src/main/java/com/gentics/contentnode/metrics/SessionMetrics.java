package com.gentics.contentnode.metrics;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.gentics.contentnode.jmx.SessionInfo;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Meter for the number of active sessions, read from the {@link SessionInfo}.
 * Since getting the number requires a database query in a transaction, the number is refreshed by a task and the gauge
 * returns the last fetched number (instead of querying the database whenever the gauge is read).
 */
public class SessionMetrics implements MeterBinder {
	/**
	 * Executor for the refresh task
	 */
	private final ScheduledExecutorService executor;

	/**
	 * Last fetched number of active sessions, NaN if unknown
	 */
	private volatile double activeSessions = Double.NaN;

	/**
	 * Create an instance
	 * @param executor executor for the refresh task
	 */
	public SessionMetrics(ScheduledExecutorService executor) {
		this.executor = executor;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		Gauge.builder("cms.sessions.active", this, s -> s.activeSessions)
			.description("Number of active sessions")
			.register(registry);

		long interval = CmsMetrics.REFRESH_INTERVAL.toMillis();
		executor.scheduleWithFixedDelay(this::refresh, 0, interval, TimeUnit.MILLISECONDS);
	}

	/**
	 * Fetch the number of active sessions
	 */
	void refresh() {
		int count = new SessionInfo().getSessionCount();
		activeSessions = count >= 0 ? count : Double.NaN;
	}
}
