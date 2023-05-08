package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.perm.PermissionStore;

/**
 * Transactional Implementation that refreshes the perm handler
 */
public class RefreshPermHandler extends AbstractTransactional {
	/**
	 * Object type
	 */
	protected int objType;

	/**
	 * Object id
	 */
	protected int objId;

	/**
	 * Group id
	 */
	protected int groupId;

	/**
	 * Global refresh perm transactional
	 */
	private static Transactional globalRefreshPermTransactional = new AbstractTransactional() {
		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transactional#onDBCommit(com.gentics.lib.base.factory.Transaction)
		 */
		public void onDBCommit(Transaction t) throws NodeException {
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transactional#onTransactionCommit(com.gentics.lib.base.factory.Transaction)
		 */
		public boolean onTransactionCommit(Transaction t) {
			try {
				PermissionStore.initialize(true);
			} catch (NodeException e) {
				PermissionStore.logger.error("Error while refreshing permission store", e);
			}
			return false;
		}
	};

	/**
	 * Create an instance to refresh the permission store for the given object
	 * @param objType object type
	 * @param objId object id
	 */
	public RefreshPermHandler(int objType, int objId) {
		this.objType = objType;
		this.objId = objId;
	}

	/**
	 * Create an instance to refresh the permission store for the given group
	 * @param groupId group id
	 */
	public RefreshPermHandler(int groupId) {
		this.groupId = groupId;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.Transactional#onDBCommit(com.gentics.lib.base.factory.Transaction)
	 */
	public void onDBCommit(Transaction t) throws NodeException {}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.Transactional#onTransactionCommit(com.gentics.lib.base.factory.Transaction)
	 */
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
			if (objType > 0) {
				PermissionStore.getInstance().refreshObject(objType, objId);
			}
			if (groupId > 0) {
				PermissionStore.getInstance().refreshGroup(groupId);
			}
		} catch (NodeException e) {
			if (objType > 0) {
				PermissionStore.logger.error("Error while refreshing permission store for object " + objType + "." + objId, e);
			}
			if (groupId > 0) {
				PermissionStore.logger.error("Error while refreshing permission store for group " + groupId, e);
			}
		}
		return false;
	}

	@Override
	public int getThreshold(Transaction t) {
		return 100;
	}

	@Override
	public Transactional getSingleton(Transaction t) {
		return globalRefreshPermTransactional;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RefreshPermHandler) {
			RefreshPermHandler other = (RefreshPermHandler) obj;
			return other.groupId == groupId && other.objId == objId && other.objType == objType;
		} else {
			return super.equals(obj);
		}
	}
}
