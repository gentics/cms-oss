package com.gentics.contentnode.metrics;

import java.util.Locale;
import java.util.function.ToIntFunction;

import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.jmx.PublisherInfo;
import com.gentics.contentnode.jmx.PublisherPhase;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Meters for the publish process, read from the {@link PublisherInfo}. The object counts are gauges, because they are reset when
 * the publish process finishes.
 */
public class PublishMetrics implements MeterBinder {
	@Override
	public void bindTo(MeterRegistry registry) {
		PublisherInfo info = MBeanRegistry.getPublisherInfo();

		Gauge.builder("cms.publish.running", info, i -> i.isRunning() ? 1 : 0)
			.description("Whether the publish process is running (1) or not (0)")
			.register(registry);

		for (PublisherPhase phase : PublisherPhase.values()) {
			Gauge.builder("cms.publish.phase", info, i -> phase.name().equals(i.getState()) ? 1 : 0)
				.description("Current phase of the publish process (1 for the current phase)")
				.tag("phase", phase.name().toLowerCase(Locale.ROOT))
				.register(registry);
		}

		registerObjects(registry, info, "page", PublisherInfo::getPagesToPublish, PublisherInfo::getPublishedPages, PublisherInfo::getRemainingPages);
		registerObjects(registry, info, "file", PublisherInfo::getFilesToPublish, PublisherInfo::getPublishedFiles, PublisherInfo::getRemainingFiles);
		registerObjects(registry, info, "folder", PublisherInfo::getFoldersToPublish, PublisherInfo::getPublishedFolders, PublisherInfo::getRemainingFolders);
		registerObjects(registry, info, "form", PublisherInfo::getFormsToPublish, PublisherInfo::getPublishedForms, PublisherInfo::getRemainingForms);
	}

	/**
	 * Register the gauges for the objects of the given type in the current publish run
	 * @param registry registry
	 * @param info publisher info
	 * @param type object type
	 * @param total function returning the total number of objects to publish
	 * @param published function returning the number of published objects
	 * @param remaining function returning the number of remaining objects
	 */
	protected void registerObjects(MeterRegistry registry, PublisherInfo info, String type, ToIntFunction<PublisherInfo> total,
			ToIntFunction<PublisherInfo> published, ToIntFunction<PublisherInfo> remaining) {
		registerObjectGauge(registry, info, type, "total", total);
		registerObjectGauge(registry, info, type, "published", published);
		registerObjectGauge(registry, info, type, "remaining", remaining);
	}

	/**
	 * Register a single gauge for objects in the current publish run
	 * @param registry registry
	 * @param info publisher info
	 * @param type object type
	 * @param status status of the counted objects
	 * @param count count function
	 */
	protected void registerObjectGauge(MeterRegistry registry, PublisherInfo info, String type, String status,
			ToIntFunction<PublisherInfo> count) {
		Gauge.builder("cms.publish.objects", info, i -> count.applyAsInt(i))
			.description("Number of objects in the current publish run")
			.tag("type", type)
			.tag("status", status)
			.register(registry);
	}
}
