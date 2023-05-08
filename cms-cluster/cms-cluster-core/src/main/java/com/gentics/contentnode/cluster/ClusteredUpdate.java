package com.gentics.contentnode.cluster;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.MaintenanceMode;
import com.gentics.contentnode.factory.Trx;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

/**
 * Cluster support for updating process
 */
public class ClusteredUpdate {
	/**
	 * Name of the Topic for update in progress in the cluster
	 */
	private final static String UPDATE_TOPIC = "clustered.update";

	/**
	 * Update message listener
	 */
	private static MessageListener<Void> UPDATE_LISTENER = msg -> {
		ClusterSupport.logger.info("Update in progress, giving up the master flag");
		ClusterSupport.giveUpMasterFlag();
	};

	/**
	 * Add message listeners
	 */
	static void addMessageListeners() {
		HazelcastInstance instance = ClusterSupport.get();

		// add listener for clustered update events
		if (instance != null) {
			ITopic<Void> updateTopic = instance.getTopic(UPDATE_TOPIC);
			updateTopic.addMessageListener(UPDATE_LISTENER);
		}
	}

	/**
	 * Inform all cluster members of an update
	 */
	public static void inform() {
		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			try (Trx trx = ContentNodeHelper.trx()) {
				MaintenanceMode.set(true, null, null);
				trx.success();
			} catch (NodeException e) {
				ClusterSupport.logger.error("Error while setting maintenance mode", e);
			}

			ITopic<Void> updateTopic = instance.getTopic(UPDATE_TOPIC);
			updateTopic.publish(null);
		}
	}
}
