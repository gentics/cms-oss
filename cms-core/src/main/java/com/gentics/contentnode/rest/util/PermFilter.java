package com.gentics.contentnode.rest.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.resource.parameter.PermsFilterParameterBean;

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
	protected Set<PermHandler.ObjectPermission> perms;

	/**
	 * Get the instance of the permission filter for the given permissions
	 * @param perms permissions
	 * @return filter
	 */
	public static <U extends NodeObject> PermFilter<U> get(PermHandler.ObjectPermission... perms) {
		return get(null, perms);
	}

	/**
	 * Get the instance of the permission filter for the given permissions, including possible filter bean contents
	 * @param filter filter bean
	 * @param perms permissions
	 * @return filter
	 */
	public static <U extends NodeObject> PermFilter<U> get(PermsFilterParameterBean filter, PermHandler.ObjectPermission... perms) {
		Set<PermHandler.ObjectPermission> parsedPerms = new HashSet<>(Arrays.asList(perms));
		if (filter != null && filter.permitted != null) {
			parsedPerms.addAll(filter.permitted.stream().map(ObjectPermission::get).filter(Objects::nonNull).collect(Collectors.toSet()));
		}
		return new PermFilter<>(parsedPerms);
	}

	/**
	 * Create an instance of the filter
	 * @param perms checked permission
	 */
	protected PermFilter(Set<PermHandler.ObjectPermission> perms) {
		this.perms = perms;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.util.NodeObjectFilter#matches(com.gentics.lib.base.object.NodeObject)
	 */
	public boolean matches(NodeObject object) throws NodeException {
		if (AbstractContentObject.isEmptyId(object.getId())) {
			return true;
		}
		for (ObjectPermission perm : perms) {
			if (!perm.checkObject(object)) {
				return false;
			}
		}
		return true;
	}
}
