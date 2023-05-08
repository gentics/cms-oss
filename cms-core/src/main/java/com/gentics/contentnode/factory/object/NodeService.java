package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;

/**
 * Interface for {@link ExtensibleObjectService} implementations for {@link Node}s containing
 * empty default implementations.
 */
public interface NodeService extends ExtensibleObjectService<Node> {
	@Override
	default void onSave(Node object, boolean isNew, boolean contentModified, int userId) throws NodeException {
	}

	@Override
	default void onDelete(Node object, boolean wastebin, int userId) throws NodeException {
	}
}
