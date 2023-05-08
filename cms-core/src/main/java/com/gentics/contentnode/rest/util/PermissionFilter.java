package com.gentics.contentnode.rest.util;

import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;

/**
 * Permission Filter.
 * Use {@link PermissionFilter#get(ObjectPermission)} to get the instance for filtering objects with the given permission.
 * <br/>Example:
 * <pre>
 * // Filter list constructs to only retain those that the user can see
 * PermissionFilter.get(ObjectPermission.view).filter(constructs);
 * </pre>
 */
public class PermissionFilter extends AbstractNodeObjectFilter {
	/**
	 * Map containing all possible PermissionFilters
	 */
	protected static Map<PermHandler.ObjectPermission, NodeObjectFilter> filters = new HashMap<PermHandler.ObjectPermission, NodeObjectFilter>();

	static {
		// create all permission filters
		for (ObjectPermission perm : ObjectPermission.values()) {
			filters.put(perm, new PermissionFilter(perm));
		}
	}

	/**
	 * Checked permission
	 */
	protected PermHandler.ObjectPermission perm;

	/**
	 * Get the instance of the permission filter for the given permission
	 * @param perm permission
	 * @return filter
	 */
	public static NodeObjectFilter get(PermHandler.ObjectPermission perm) {
		return filters.get(perm);
	}

	/**
	 * Create an instance of the filter
	 * @param perm checked permission
	 */
	protected PermissionFilter(PermHandler.ObjectPermission perm) {
		this.perm = perm;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.util.NodeObjectFilter#matches(com.gentics.lib.base.object.NodeObject)
	 */
	public boolean matches(NodeObject object) throws NodeException {
		return perm.checkObject(object);
	}
}
