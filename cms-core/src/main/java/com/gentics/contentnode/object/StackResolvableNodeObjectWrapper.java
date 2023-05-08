package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;

/**
 * Abstract wrapper for {@link StackResolvableNodeObject} objects
 *
 * @param <U> type
 */
public abstract class StackResolvableNodeObjectWrapper<U extends StackResolvableNodeObject> implements StackResolvableNodeObject {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = -8453950605130765269L;

	/**
	 * Wrapped object
	 */
	protected U wrapped;

	/**
	 * Create instance with wrapped object
	 * @param wrapped wrapped object
	 */
	protected StackResolvableNodeObjectWrapper(U wrapped) {
		this.wrapped = wrapped;
	}

	/**
	 * Get the wrapped object
	 * @return wrapped object
	 */
	public U getWrapped() {
		return wrapped;
	}

	@Override
	public Integer getId() {
		return wrapped.getId();
	}

	@Override
	public NodeFactory getFactory() {
		return wrapped.getFactory();
	}

	@Override
	public NodeObjectInfo getObjectInfo() {
		return wrapped.getObjectInfo();
	}

	@Override
	public Integer getTType() {
		return wrapped.getTType();
	}

	@Override
	public void triggerEvent(DependencyObject object, String[] property, int eventMask, int depth, int channelId)
			throws NodeException {
		wrapped.triggerEvent(object, property, eventMask, depth, channelId);
	}

	@Override
	public void delete() throws InsufficientPrivilegesException, NodeException {
		wrapped.delete();
	}

	@Override
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
		wrapped.delete(force);
	}

	@Override
	public boolean isDeleted() {
		return wrapped.isDeleted();
	}

	@Override
	public int getDeleted() {
		return wrapped.getDeleted();
	}

	@Override
	public SystemUser getDeletedBy() throws NodeException {
		return wrapped.getDeletedBy();
	}

	@Override
	public void restore() throws NodeException {
		wrapped.restore();
	}

	@Override
	public boolean save(Integer userGroupId) throws InsufficientPrivilegesException, NodeException {
		return wrapped.save(userGroupId);
	}

	@Override
	public boolean save() throws InsufficientPrivilegesException, NodeException {
		return wrapped.save();
	}

	@Override
	public void unlock() throws NodeException {
		wrapped.unlock();
	}

	@Override
	public void dirtCache() throws NodeException {
		wrapped.dirtCache();
	}

	@Override
	public NodeObject getParentObject() throws NodeException {
		return wrapped.getParentObject();
	}

	@Override
	public NodeObject copy() throws NodeException {
		return wrapped.copy();
	}

	@Override
	public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
		wrapped.copyFrom(original);
	}

	@Override
	public NodeObject getPublishedObject() throws NodeException {
		return wrapped.getPublishedObject();
	}

	@Override
	public int getUdate() {
		return wrapped.getUdate();
	}

	@Override
	public int getEffectiveUdate() throws NodeException {
		return wrapped.getEffectiveUdate();
	}

	@Override
	public GlobalId getGlobalId() {
		return wrapped.getGlobalId();
	}

	@Override
	public void setGlobalId(GlobalId globalId) throws ReadOnlyException, NodeException {
		wrapped.setGlobalId(globalId);
	}

	@Override
	public Resolvable getKeywordResolvable(String keyword) throws NodeException {
		return wrapped.getKeywordResolvable(keyword);
	}

	@Override
	public Resolvable getShortcutResolvable() throws NodeException {
		return wrapped.getShortcutResolvable();
	}

	@Override
	public String getStackHashKey() {
		return wrapped.getStackHashKey();
	}

	@Override
	public String[] getStackKeywords() {
		return wrapped.getStackKeywords();
	}
}
