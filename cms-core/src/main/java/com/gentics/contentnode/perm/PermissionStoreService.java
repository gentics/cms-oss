package com.gentics.contentnode.perm;

import com.gentics.api.lib.exception.NodeException;

/**
 * Interface for implementations, which handle refresh/remove operations on the {@link PermissionStore}
 */
public interface PermissionStoreService {
	/**
	 * Called when a group has been refreshed in the {@link PermissionStore}
	 * @param groupId group ID
	 * @throws NodeException
	 */
	void refreshGroup(final int groupId) throws NodeException;

	/**
	 * Called when an object has been refreshed in the {@link PermissionStore}
	 * @param objType object type
	 * @param objId object ID
	 * @throws NodeException
	 */
	void refreshObject(final int objType, final int objId) throws NodeException;

	/**
	 * Called when a role has been refreshed in the {@link PermissionStore}
	 * @param roleId role ID
	 * @throws NodeException
	 */
	void refreshRole(final int roleId) throws NodeException;

	/**
	 * Called when a group has been removed from the {@link PermissionStore}
	 * @param groupId group ID
	 * @throws NodeException
	 */
	void removeGroup(int groupId) throws NodeException;

	/**
	 * Called when an object has been removed from the {@link PermissionStore}
	 * @param objType object type
	 * @param objId object ID
	 */
	void removeObject(final int objType, final int objId);

	/**
	 * Called when a role has been removed from the {@link PermissionStore}
	 * @param roleId role ID
	 */
	void removeRole(final int roleId);
}
