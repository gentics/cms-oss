package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.perm.PermissionStore;

/**
 * Transactional that will refresh the permissions for a role
 */
public class RefreshRoleHandler extends AbstractTransactional {
	protected int roleId;

	/**
	 * Create instance
	 * @param roleId role ID
	 */
	public RefreshRoleHandler(int roleId) {
		this.roleId = roleId;
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
			PermissionStore.getInstance().refreshRole(roleId);
		} catch (NodeException e) {
			PermissionStore.logger.error("Error while refreshing permission store for role " + roleId, e);
		}

		return false;
	}
}
