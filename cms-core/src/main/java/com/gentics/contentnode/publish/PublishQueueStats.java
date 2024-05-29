package com.gentics.contentnode.publish;

import static com.gentics.contentnode.factory.Trx.operate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.PrefixedThreadFactory;
import com.gentics.contentnode.jmx.PublisherInfo;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.response.PublishQueueCounts;
import com.gentics.contentnode.rest.model.response.admin.ObjectCount;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of publish queue statistics, which are refreshed in a fixed interval
 */
public class PublishQueueStats {
	/**
	 * Logger instance
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(PublishQueueStats.class);

	/**
	 * Singleton
	 */
	private static PublishQueueStats singleton = new PublishQueueStats();

	/**
	 * Thread factory
	 */
	private static ThreadFactory threadFactory = new PrefixedThreadFactory("publishqueue-stats");

	/**
	 * Service, once the instance has been initialized
	 */
	private ScheduledExecutorService service = null;

	/**
	 * Future of the scheduled job to refresh the stats
	 */
	private ScheduledFuture<?> scheduled;

	/**
	 * Current count of delayed entries in publish queue per node and object type
	 */
	private Map<Integer, Map<Integer, Integer>> delayedCounts = Collections.emptyMap();

	/**
	 * Current count of not delayed entries in publish queue per node and object type
	 */
	private Map<Integer, Map<Integer, Integer>> notDelayedCounts = Collections.emptyMap();

	/**
	 * Delay of the scheduled job in ms
	 */
	private long delayMs;

	/**
	 * Get the singleton instance
	 * @return instance
	 */
	public static PublishQueueStats get() {
		return singleton;
	}

	/**
	 * Private constructor for singleton
	 */
	private PublishQueueStats() {
	}

	/**
	 * Initialize refreshing the stats in the given delay (in milliseconds)
	 * @param delayMs delay in ms
	 */
	public void init(long delayMs) {
		if (service != null && !service.isShutdown() && scheduled != null && !scheduled.isDone()
				&& this.delayMs == delayMs) {
			return;
		}
		this.delayMs = delayMs;
		shutdown();
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Schedule refreshing of publish queue statistics with delay of %d ms", delayMs));
		}
		service = Executors.newSingleThreadScheduledExecutor(threadFactory);
		scheduled = service.scheduleWithFixedDelay(() -> {
			refresh();
		}, 0, delayMs, TimeUnit.MILLISECONDS);
	}

	/**
	 * Shutdown the scheduled job for refreshing the stats
	 */
	public void shutdown() {
		if (scheduled != null && !scheduled.isDone()) {
			scheduled.cancel(true);
		}
		scheduled = null;
		if (service != null && !service.isShutdown()) {
			if (logger.isInfoEnabled()) {
				logger.info("Stop refreshing of publish queue statistics");
			}
			service.shutdown();
			try {
				service.awaitTermination(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				logger.warn("Error while waiting for service shutdown", e);
			}
		}
		service = null;
	}

	/**
	 * Refresh the statistics
	 */
	public void refresh() {
		try {
			long start = System.currentTimeMillis();
			if (logger.isDebugEnabled()) {
				logger.debug("Refreshing publish queue statistics");
			}
			operate(() -> {
				Map<Integer, Map<Integer, Integer>> newDelayedCounts = new HashMap<>();
				Map<Integer, Map<Integer, Integer>> newNotDelayedCounts = new HashMap<>();

				DBUtils.select("SELECT COUNT(DISTINCT pq.obj_id) c, pq.channel_id, pq.obj_type, pq.delay FROM publishqueue pq WHERE pq.action NOT IN (?, ?, ?, ?) GROUP BY pq.channel_id, pq.obj_type, pq.delay", stmt -> {
					int index = 1;
					stmt.setString(index++, PublishQueue.Action.DELETE.toString()); // pq.action NOT IN (?, ?, ?, ?)
					stmt.setString(index++, PublishQueue.Action.HIDE.toString()); // pq.action NOT IN (?, ?, ?, ?)
					stmt.setString(index++, PublishQueue.Action.REMOVE.toString()); // pq.action NOT IN (?, ?, ?, ?)
					stmt.setString(index++, PublishQueue.Action.OFFLINE.toString()); // pq.action NOT IN (?, ?, ?, ?)
				}, rs -> {
					while (rs.next()) {
						int count = rs.getInt("c");
						int nodeId = rs.getInt("channel_id");
						int objType = rs.getInt("obj_type");
						boolean delay = rs.getBoolean("delay");

						if (delay) {
							newDelayedCounts.computeIfAbsent(nodeId, k -> new HashMap<>()).put(objType, count);
						} else {
							newNotDelayedCounts.computeIfAbsent(nodeId, k -> new HashMap<>()).put(objType, count);
						}
					}
					return null;
				});

				delayedCounts = Collections.unmodifiableMap(newDelayedCounts);
				notDelayedCounts = Collections.unmodifiableMap(newNotDelayedCounts);
			});
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Refreshed publish queue statistics in %d ms", System.currentTimeMillis() - start));
			}
		} catch (Throwable e) {
			logger.error("Error while refreshing publish queue statistics", e);
		}
	}

	/**
	 * Get object counts for the given object type
	 * @param objType object type
	 * @param toPublishCount function to get the count of objects to publish
	 * @param publishedCount function to get the count of published objects
	 * @param remainingCount function to get the count of remaining objects
	 * @param nodeIds optional node IDs
	 * @return object count instance
	 */
	public ObjectCount count(int objType, Function<Integer, Integer> toPublishCount, Function<Integer, Integer> publishedCount, Function<Integer, Integer> remainingCount, int...nodeIds) {
		ObjectCount count = new ObjectCount();
		if (toPublishCount == null) {
			toPublishCount = nodeId -> count(nodeId, objType, false);
		}
		for (int nodeId : nodeIds) {
			count.setDelayed(count.getDelayed() + count(nodeId, objType, true));
			count.setToPublish(count.getToPublish() + toPublishCount.apply(nodeId));
			count.setPublished(count.getPublished() + publishedCount.apply(nodeId));
			count.setRemaining(count.getRemaining() + remainingCount.apply(nodeId));
		}
		return count;
	}

	/**
	 * Get the counts for all object types from the publish queue for the given node
	 * @param nodeId node ID
	 * @param publisherInfo publisher info instance
	 * @return counts
	 */
	public PublishQueueCounts counts(int nodeId, PublisherInfo publisherInfo) {
		return new PublishQueueCounts()
				.setFiles(count(File.TYPE_FILE, publisherInfo.isRunning() ? publisherInfo::getFilesToPublish : null, publisherInfo::getPublishedFiles, publisherInfo::getRemainingFiles, nodeId))
				.setFolders(count(Folder.TYPE_FOLDER, publisherInfo.isRunning() ? publisherInfo::getFoldersToPublish : null, publisherInfo::getPublishedFolders, publisherInfo::getRemainingFolders, nodeId))
				.setForms(count(Form.TYPE_FORM, publisherInfo.isRunning() ? publisherInfo::getFormsToPublish : null, publisherInfo::getPublishedForms, publisherInfo::getRemainingForms, nodeId))
				.setPages(count(Page.TYPE_PAGE, publisherInfo.isRunning() ? publisherInfo::getPagesToPublish : null, publisherInfo::getPublishedPages, publisherInfo::getRemainingPages, nodeId));
	}

	/**
	 * Get the count for a single node and object type
	 * @param nodeId node ID
	 * @param objType object type
	 * @param delayed true to get delayed count, false for not delayed count
	 * @return number of entries in the publish queue
	 */
	public int count(int nodeId, int objType, boolean delayed) {
		if (delayed) {
			return delayedCounts.getOrDefault(nodeId, Collections.emptyMap()).getOrDefault(objType, 0);
		} else {
			return notDelayedCounts.getOrDefault(nodeId, Collections.emptyMap()).getOrDefault(objType, 0);
		}
	}
}
