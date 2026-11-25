package com.gentics.contentnode.metrics;

import java.util.Optional;
import java.util.function.ToIntFunction;

import org.apache.commons.pool.impl.GenericObjectPool;

import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Meters for the pool of backend database connections. The pool is resolved whenever a gauge is read, because it is replaced
 * when the configuration is reloaded. The gauges return NaN while there is no pool.
 */
public class ConnectionPoolMetrics implements MeterBinder {
	@Override
	public void bindTo(MeterRegistry registry) {
		Gauge.builder("cms.db.pool.connections", () -> get(GenericObjectPool::getNumActive))
			.description("Number of connections in the pool, which are currently in use (active) or idle")
			.tag("state", "active")
			.register(registry);
		Gauge.builder("cms.db.pool.connections", () -> get(GenericObjectPool::getNumIdle))
			.description("Number of connections in the pool, which are currently in use (active) or idle")
			.tag("state", "idle")
			.register(registry);
		Gauge.builder("cms.db.pool.max.active", () -> get(GenericObjectPool::getMaxActive))
			.description("Maximum number of active connections in the pool (negative for no limit)")
			.register(registry);
		Gauge.builder("cms.db.pool.max.idle", () -> get(GenericObjectPool::getMaxIdle))
			.description("Maximum number of idle connections in the pool (negative for no limit)")
			.register(registry);
	}

	/**
	 * Get a value from the current pool. Does not create the configuration, if it does not exist (e.g. during shutdown).
	 * @param value value function
	 * @return value, NaN if there is no pool
	 */
	protected Number get(ToIntFunction<GenericObjectPool<Object>> value) {
		Optional<GenericObjectPool<Object>> pool = NodeConfigRuntimeConfiguration.peek().flatMap(NodeConfig::getConnectionPool);
		return pool.<Number>map(value::applyAsInt).orElse(Double.NaN);
	}
}
