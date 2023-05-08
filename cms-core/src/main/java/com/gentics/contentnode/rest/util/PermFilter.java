package com.gentics.contentnode.rest.util;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;

/**
 * Permission Filter.
 * Use {@link PermFilter#get(ObjectPermission)} to get the instance for filtering objects with the given permission.
 * <br/>Example:
 * <pre>
 * // Filter list constructs to only retain those that the user can see
 * PermFilter.get(ObjectPermission.view).filter(constructs);
 * </pre>
 */
public class PermFilter<T extends NodeObject> implements Filter<T> {
	/**
	 * Checked permission
	 */
	protected PermHandler.ObjectPermission perm;

	/**
	 * Get the instance of the permission filter for the given permission
	 * @param perm permission
	 * @return filter
	 */
	public static <U extends NodeObject> PermFilter<U> get(PermHandler.ObjectPermission perm) {
		return new PermFilter<>(perm);
	}

	/**
	 * Create an instance of the filter
	 * @param perm checked permission
	 */
	protected PermFilter(PermHandler.ObjectPermission perm) {
		this.perm = perm;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.util.NodeObjectFilter#matches(com.gentics.lib.base.object.NodeObject)
	 */
	public boolean matches(NodeObject object) throws NodeException {
		return AbstractContentObject.isEmptyId(object.getId()) || perm.checkObject(object);
	}
}
