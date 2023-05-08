package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.perm.PermissionStore;

/**
 * Transactional for removing the permissionstore for a group
 */
public class TransactionalRemoveGroupPermissionStore extends AbstractTransactional {
	/**
	 * Group id
	 */
	protected int groupId;

	/**
	 * Create an instance to remove the permission store for the given group
	 * @param groupId group id
	 */
	public TransactionalRemoveGroupPermissionStore(int groupId) {
		this.groupId = groupId;
	}

	@Override
	public void onDBCommit(Transaction t) throws NodeException {
	}

	@Override
	public boolean onTransactionCommit(Transaction t) {
		// Getting the PermissionStore instance will throw an exception, if
		// the PermissionStore has not yet been initialized, which is ok,
		// we just ignore it
		try {
			PermissionStore.getInstance();
		} catch (NodeException e) {
			return false;
		}
		try {
			PermissionStore.getInstance().removeGroup(groupId);
		} catch (NodeException e) {
			PermissionStore.logger.error("Error while removing permission store for group " + groupId, e);
		}
		return false;
	}
}
