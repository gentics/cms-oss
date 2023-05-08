package com.gentics.contentnode.cluster;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.perm.PermissionStoreService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

/**
 * Cluster specific implementation of {@link PermissionStoreService} which will let all cluster instances refresh their stores
 */
public class ClusterPermissionStoreService implements PermissionStoreService {
	/**
	 * Name of the Topic for permission store synchronization in the cluster
	 */
	private final static String PERM_SYNC_TOPIC = "perm.sync";

	/**
	 * Permission Synchronization message listener
	 */
	private static MessageListener<PermSyncMessage> PERM_SYNC_LISTENER = msg -> {
		ClusterSupport.executeIfNotFromLocal(msg, m -> {
			m.execute();
		});
	};

	@Override
	public void refreshGroup(int groupId) throws NodeException {
		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			ITopic<PermSyncMessage> topic = instance.getTopic(PERM_SYNC_TOPIC);
			topic.publish(new PermSyncMessage().setType(PermSyncMessageType.refreshGroup).setGroupId(groupId));
		}
	}

	@Override
	public void refreshObject(int objType, int objId) throws NodeException {
		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			ITopic<PermSyncMessage> topic = instance.getTopic(PERM_SYNC_TOPIC);
			topic.publish(new PermSyncMessage().setType(PermSyncMessageType.refreshObject).setObjectType(objType).setObjectId(objId));
		}
	}

	@Override
	public void refreshRole(int roleId) throws NodeException {
		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			ITopic<PermSyncMessage> topic = instance.getTopic(PERM_SYNC_TOPIC);
			topic.publish(new PermSyncMessage().setType(PermSyncMessageType.refreshRole).setRoleId(roleId));
		}
	}

	@Override
	public void removeGroup(int groupId) throws NodeException {
		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			ITopic<PermSyncMessage> topic = instance.getTopic(PERM_SYNC_TOPIC);
			topic.publish(new PermSyncMessage().setType(PermSyncMessageType.removeGroup).setGroupId(groupId));
		}
	}

	@Override
	public void removeObject(int objType, int objId) {
		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			ITopic<PermSyncMessage> topic = instance.getTopic(PERM_SYNC_TOPIC);
			topic.publish(new PermSyncMessage().setType(PermSyncMessageType.removeObject).setObjectType(objType).setObjectId(objId));
		}
	}

	@Override
	public void removeRole(int roleId) {
		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			ITopic<PermSyncMessage> topic = instance.getTopic(PERM_SYNC_TOPIC);
			topic.publish(new PermSyncMessage().setType(PermSyncMessageType.removeRole).setRoleId(roleId));
		}
	}

	/**
	 * Reinitialize the permission stores in the cluster
	 */
	public static void init() {
		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			ITopic<PermSyncMessage> topic = instance.getTopic(PERM_SYNC_TOPIC);
			topic.publish(new PermSyncMessage().setType(PermSyncMessageType.init));
		}
	}

	/**
	 * Add message listeners
	 */
	static void addMessageListeners() {
		HazelcastInstance instance = ClusterSupport.get();
		if (instance != null) {
			// add listener for perm sync events
			ITopic<PermSyncMessage> permSyncTopic = instance.getTopic(PERM_SYNC_TOPIC);
			permSyncTopic.addMessageListener(PERM_SYNC_LISTENER);
		}
	}
}
