package com.gentics.contentnode.object;

import com.gentics.contentnode.object.NodeObject.GlobalId;

/**
 * Abstract wrapper implementation for implementations of {@link StagedReferencedObject}
 *
 * @param <T> node object type
 */
public abstract class AbstractStagedReferencedObject<T extends NodeObject> implements StagedReferencedObject<T> {
	/**
	 * Wrapped object
	 */
	protected T wrapped;

	/**
	 * Create a wrapper
	 * @param wrapped wrapped object
	 */
	public AbstractStagedReferencedObject(T wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public GlobalId getGlobalId() {
		return wrapped.getGlobalId();
	}
}
