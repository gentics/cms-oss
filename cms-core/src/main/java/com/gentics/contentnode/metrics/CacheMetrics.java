package com.gentics.contentnode.metrics;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

import org.apache.jcs.engine.control.CompositeCache;
import org.apache.jcs.engine.control.CompositeCacheManager;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Meters for the JCS cache regions. Since cache regions are created on demand, the meters for new regions are registered
 * by a task, which periodically checks for new regions.
 */
public class CacheMetrics implements MeterBinder {
	/**
	 * Executor for the task checking for new regions
	 */
	private final ScheduledExecutorService executor;

	/**
	 * Cache manager. Must be the instance configured by the CMS (see {@code GCMSCache#initialize(String)}), so this
	 * must not be created before the caches are initialized.
	 */
	private final CompositeCacheManager cacheManager;

	/**
	 * Names of the regions, for which the meters have been registered
	 */
	private final Set<String> boundRegions = ConcurrentHashMap.newKeySet();

	/**
	 * Registry, set when bound
	 */
	private MeterRegistry registry;

	/**
	 * Create an instance
	 * @param executor executor for the task checking for new regions
	 */
	public CacheMetrics(ScheduledExecutorService executor) {
		this.executor = executor;
		this.cacheManager = CompositeCacheManager.getInstance();
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		this.registry = registry;
		bindNewRegions();

		long interval = CmsMetrics.REFRESH_INTERVAL.toMillis();
		executor.scheduleWithFixedDelay(this::bindNewRegions, interval, interval, TimeUnit.MILLISECONDS);
	}

	/**
	 * Register the meters for all regions, for which they have not been registered yet
	 */
	synchronized void bindNewRegions() {
		for (String region : cacheManager.getCacheNames()) {
			if (boundRegions.add(region)) {
				bindRegion(region, cacheManager.getCache(region));
			}
		}
	}

	/**
	 * Register the meters for the given region
	 * @param region region name
	 * @param cache cache of the region
	 */
	protected void bindRegion(String region, CompositeCache cache) {
		Gauge.builder("cms.cache.size", cache, CompositeCache::getSize)
			.description("Number of objects in the memory cache of the region")
			.tag("region", region)
			.register(registry);

		bindGets(region, cache, "hit_ram", CompositeCache::getHitCountRam);
		bindGets(region, cache, "hit_aux", CompositeCache::getHitCountAux);
		bindGets(region, cache, "miss_notfound", CompositeCache::getMissCountNotFound);
		bindGets(region, cache, "miss_expired", CompositeCache::getMissCountExpired);
	}

	/**
	 * Register the counter for cache gets with the given result
	 * @param region region name
	 * @param cache cache of the region
	 * @param result result of the gets
	 * @param count count function
	 */
	protected void bindGets(String region, CompositeCache cache, String result, ToDoubleFunction<CompositeCache> count) {
		FunctionCounter.builder("cms.cache.gets", cache, count)
			.description("Number of gets from the cache region by result (hit in memory or auxiliary cache, miss because not found or expired)")
			.tag("region", region)
			.tag("result", result)
			.register(registry);
	}
}
