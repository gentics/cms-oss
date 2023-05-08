package com.gentics.contentnode.cluster;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.publish.InstantPublishService;
import com.gentics.contentnode.publish.PublishController;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

/**
 * Cluster support for instant publishing
 */
public class ClusterInstantPublishService implements InstantPublishService {
	/**
	 * Name of the Topic for synchronization of instant publishing with the publish process
	 */
	private final static String INSTANT_PUBLISH_SYNC_TOPIC = "instant.publish.sync";

	/**
	 * Instant Publishing Synchronization message listener
	 */
	private static MessageListener<InstantPublishSyncMessage> INSTANT_PUBLISH_SYNC_LISTENER = msg -> {
		if (ClusterSupport.isMaster()) {
			try {
				Trx.operate(t -> {
					InstantPublishSyncMessage syncMsg = msg.getMessageObject();
					NodeObject object = t.getObject(t.getClass(syncMsg.getObjectType()), syncMsg.getObjectId());
					if (object != null) {
						if (syncMsg.isFailed()) {
							PublishController.instantPublishFailed(object);
						} else {
							PublishController.instantPublished(object);
						}
					}
				});
			} catch (NodeException e) {
				ClusterSupport.logger.error("Error while handling instant publish sync message", e);
			}
		}
	};

	/**
	 * Publish message about instant publishing, if the instance is not the master
	 * @param objType object type
	 * @param objId object ID
	 */
	@Override
	public void succeeded(int objType, int objId) {
		if (!ClusterSupport.isMaster()) {
			HazelcastInstance instance = ClusterSupport.get();
			if (instance != null) {
				ITopic<InstantPublishSyncMessage> topic = instance.getTopic(INSTANT_PUBLISH_SYNC_TOPIC);
				topic.publish(new InstantPublishSyncMessage().setFailed(false).setObjectType(objType).setObjectId(objId));
			}
		}
	}

	/**
	 * Publish message about failed instant publishing, if the instance is not the master
	 * @param objType object type
	 * @param objId object ID
	 */
	@Override
	public void failed(int objType, int objId) {
		if (!ClusterSupport.isMaster()) {
			HazelcastInstance instance = ClusterSupport.get();
			if (instance != null) {
				ITopic<InstantPublishSyncMessage> topic = instance.getTopic(INSTANT_PUBLISH_SYNC_TOPIC);
				topic.publish(new InstantPublishSyncMessage().setFailed(true).setObjectType(objType).setObjectId(objId));
			}
		}
	}

	/**
	 * Add message listeners
	 */
	static void addMessageListeners() {
		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			// add listener for instant publishing sync events
			ITopic<InstantPublishSyncMessage> instantPublishSyncTopic = instance.getTopic(INSTANT_PUBLISH_SYNC_TOPIC);
			instantPublishSyncTopic.addMessageListener(INSTANT_PUBLISH_SYNC_LISTENER);
		}
	}
}
