package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.ObjectReadOnlyException;

/**
 * Interface for implementations of {@link NodeObject} that contain a REST model
 *
 * @param <T> type of the REST model
 */
public interface NodeObjectWithModel<T> extends NodeObject {
	/**
	 * Get the REST model
	 * @return REST model
	 */
	T getModel();

	/**
	 * Fill the data from the model into this instance (if it is editable)
	 * @param model model
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	default void fromModel(T model) throws ReadOnlyException, NodeException {
		throw new ObjectReadOnlyException(this);
	}
}
