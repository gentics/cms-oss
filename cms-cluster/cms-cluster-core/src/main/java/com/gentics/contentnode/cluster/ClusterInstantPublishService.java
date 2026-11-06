package com.gentics.contentnode.cluster;

import java.util.function.Supplier;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.publish.InstantPublishService;
import com.gentics.contentnode.publish.PublishController;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.MessageListener;

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
		if (ClusterSupport.getSingleton().isMaster()) {
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
					if (ClusterSupport.logger.isDebugEnabled()) {
						ClusterSupport.logger.debug(String.format("Handled %s", syncMsg));
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
		publishMessage(() -> new InstantPublishSyncMessage().setFailed(false).setObjectType(objType).setObjectId(objId));
	}

	/**
	 * Publish message about failed instant publishing, if the instance is not the master
	 * @param objType object type
	 * @param objId object ID
	 */
	@Override
	public void failed(int objType, int objId) {
		publishMessage(() -> new InstantPublishSyncMessage().setFailed(true).setObjectType(objType).setObjectId(objId));
	}

	/**
	 * If allowed, publish the message supplied by the given message supplier
	 * @param msgSupplier message supplier
	 */
	protected void publishMessage(Supplier<InstantPublishSyncMessage> msgSupplier) {
		if (!ClusterSupport.getSingleton().isMaster()) {
			HazelcastInstance instance = ClusterSupport.getSingleton().get();
			if (instance != null) {
				ITopic<InstantPublishSyncMessage> topic = instance.getTopic(INSTANT_PUBLISH_SYNC_TOPIC);
				InstantPublishSyncMessage msg = msgSupplier.get();
				topic.publish(msg);
				if (ClusterSupport.logger.isDebugEnabled()) {
					ClusterSupport.logger.debug(String.format("Published %s", msg));
				}
			}
		}
	}

	/**
	 * Add message listeners
	 */
	static void addMessageListeners() {
		HazelcastInstance instance = ClusterSupport.getSingleton().get();
		if (instance != null) {
			// add listener for instant publishing sync events
			ITopic<InstantPublishSyncMessage> instantPublishSyncTopic = instance.getTopic(INSTANT_PUBLISH_SYNC_TOPIC);
			instantPublishSyncTopic.addMessageListener(INSTANT_PUBLISH_SYNC_LISTENER);
		}
	}
}
