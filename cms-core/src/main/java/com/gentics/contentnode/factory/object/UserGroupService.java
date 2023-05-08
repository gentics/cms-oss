package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.UserGroup;

/**
 * Interface for {@link ExtensibleObjectService} implementations for {@link UserGroup}s containing
 * empty default implementations.
 */
public interface UserGroupService extends ExtensibleObjectService<UserGroup> {
	@Override
	default void onSave(UserGroup object, boolean isNew, boolean contentModified, int userId) throws NodeException {
	}

	@Override
	default void onDelete(UserGroup object, boolean wastebin, int userId) throws NodeException {
	}
}
