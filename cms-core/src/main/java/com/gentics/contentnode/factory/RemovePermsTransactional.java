package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.perm.PermissionStore;

/**
 * Transactional Implementation that removes the PermissionStore entries for a
 * given object
 */
public class RemovePermsTransactional extends AbstractTransactional {
	/**
	 * Object type
	 */
	protected int objType;

	/**
	 * Object id
	 */
	protected int objId;

	/**
	 * Create an instance
	 * @param objType object type
	 * @param objId object id
	 */
	public RemovePermsTransactional(int objType, int objId) {
		this.objType = objType;
		this.objId = objId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gentics.lib.base.factory.Transactional#onDBCommit(com.gentics.lib
	 * .base.factory.Transaction)
	 */
	public void onDBCommit(Transaction t) throws NodeException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gentics.lib.base.factory.Transactional#onTransactionCommit(com.gentics
	 * .lib.base.factory.Transaction)
	 */
	public boolean onTransactionCommit(Transaction t) {
		try {
			if (objType > 0 && objId > 0) {
				PermissionStore.getInstance().removeObject(objType, objId);
			}
		} catch (NodeException ignored) {
		}
		return false;
	}
}
