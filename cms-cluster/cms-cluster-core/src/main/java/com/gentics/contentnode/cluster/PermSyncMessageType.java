package com.gentics.contentnode.cluster;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.perm.PermissionStore;

/**
 * Types of {@link PermSyncMessage}
 */
public enum PermSyncMessageType {
	/**
	 * Refresh perms for group
	 */
	refreshGroup(msg -> PermissionStore.getInstance().refreshGroupLocal(msg.getGroupId())),

	/**
	 * Refresh perms for role
	 */
	refreshRole(msg -> PermissionStore.getInstance().refreshRoleLocal(msg.getRoleId())),

	/**
	 * Refresh perms for object
	 */
	refreshObject(msg -> PermissionStore.getInstance().refreshObjectLocal(msg.getObjectType(), msg.getObjectId())),

	/**
	 * Remove perms for group
	 */
	removeGroup(msg -> PermissionStore.getInstance().removeGroupLocal(msg.getGroupId())),

	/**
	 * Remove perms for role
	 */
	removeRole(msg -> PermissionStore.getInstance().removeRoleLocal(msg.getRoleId())),

	/**
	 * Remove perms for object
	 */
	removeObject(msg -> PermissionStore.getInstance().removeObjectLocal(msg.getObjectType(), msg.getObjectId())),

	/**
	 * Reinitialize the PermissionStore
	 */
	init(msg -> PermissionStore.initialize(true));

	/**
	 * Message handler
	 */
	private Consumer<PermSyncMessage> handler;

	/**
	 * Create instance
	 * @param handler message handler
	 */
	private PermSyncMessageType(Consumer<PermSyncMessage> handler) {
		this.handler = handler;
	}

	/**
	 * Execute the handler on the given message
	 * @param msg message
	 */
	void execute(PermSyncMessage msg) {
		try (Trx trx = new Trx()) {
			handler.accept(msg);
			trx.success();
			if (ClusterSupport.logger.isDebugEnabled()) {
				ClusterSupport.logger.debug(String.format("Handled %s", msg));
			}
		} catch (NodeException e) {
			ClusterSupport.logger.error(String.format("Error while handling perm sync message %s", msg), e);
		}
	}
}
