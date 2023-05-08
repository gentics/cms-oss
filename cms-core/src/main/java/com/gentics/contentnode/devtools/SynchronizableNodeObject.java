package com.gentics.contentnode.devtools;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.object.NodeObject;

/**
 * Interface for {@link NodeObject} implementations that can be synchronized to the filesystem
 */
public interface SynchronizableNodeObject extends NodeObject, Resolvable {
	/**
	 * Get the master object if it has one, or the object itself, it is has no master
	 * @return master object or this
	 */
	default SynchronizableNodeObject getMaster() throws NodeException {
		return this;
	}

	/**
	 * Return true if the object itself is a master object
	 * @return true for master objects
	 * @throws NodeException
	 */
	default boolean isMaster() throws NodeException {
		return true;
	}
}
