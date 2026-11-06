package com.gentics.contentnode.cluster;

import java.util.Set;
import java.util.function.Supplier;

import com.gentics.contentnode.cache.CacheService;
import com.gentics.contentnode.distributed.DistributionSync;
import com.gentics.contentnode.object.NodeObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.MessageListener;

/**
 * Cluster specific implementation of {@link CacheService} which will send messages to clear or dirt caches on all cluster instances
 */
public class ClusterCacheService implements CacheService {
	/**
	 * Name of the Topic for cache synchronization in the cluster
	 */
	private final static String CACHE_SYNC_TOPIC = "cache.sync";

	/**
	 * Sync object to avoid distributing the clearCache call to other members if running, because it was distributed from another member before
	 */
	private final static DistributionSync sync = new DistributionSync();

	/**
	 * Cache Synchronization message listener
	 */
	private static MessageListener<CacheSyncMessage> CACHE_SYNC_LISTENER = msg -> {
		ClusterSupport.executeIfNotFromLocal(msg, m -> {
			try (DistributionSync.Trx trc = sync.get()) {
				m.execute();
			}
		});
	};

	@Override
	public void clear() {
		publishMessage(() -> new CacheSyncMessage().setType(CacheSyncMessageType.clearall));
	}

	@Override
	public void clear(Class<? extends NodeObject> clazz) {
		publishMessage(() -> new CacheSyncMessage().setType(CacheSyncMessageType.cleartype).setClazz(clazz));
	}

	@Override
	public void clear(Class<? extends NodeObject> clazz, Integer id) {
		publishMessage(() -> new CacheSyncMessage().setType(CacheSyncMessageType.clearobject).setClazz(clazz).setId(id));
	}

	@Override
	public void clear(Class<? extends NodeObject> clazz, Set<Integer> ids) {
		publishMessage(() -> new CacheSyncMessage().setType(CacheSyncMessageType.clearobjects).setClazz(clazz).setIds(ids));
	}

	@Override
	public void dirt(Class<? extends NodeObject> clazz, Integer id) {
		publishMessage(() -> new CacheSyncMessage().setType(CacheSyncMessageType.dirt).setClazz(clazz).setId(id));
	}

	/**
	 * If allowed, publish the message which is supplied by the given message supplier
	 * @param msgSupplier message supplier
	 */
	protected void publishMessage(Supplier<CacheSyncMessage> msgSupplier) {
		HazelcastInstance instance = ClusterSupport.getSingleton().get();
		if (instance != null && sync.allow()) {
			ITopic<CacheSyncMessage> cacheSyncTopic = instance.getTopic(CACHE_SYNC_TOPIC);
			CacheSyncMessage msg = msgSupplier.get();
			cacheSyncTopic.publish(msg);
			if (ClusterSupport.logger.isDebugEnabled()) {
				ClusterSupport.logger.debug(String.format("Published %s", msg));
			}
		}
	}

	/**
	 * Add message listeners
	 */
	static void addMessageListeners() {
		HazelcastInstance instance = ClusterSupport.getSingleton().get();

		// add listener for cache put/clear events
		if (instance != null) {
			ITopic<CacheSyncMessage> cacheSyncTopic = instance.getTopic(CACHE_SYNC_TOPIC);
			cacheSyncTopic.addMessageListener(CACHE_SYNC_LISTENER);
		}
	}
}
