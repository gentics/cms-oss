package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.perm.RoleItem;
import com.gentics.contentnode.rest.model.perm.TypePermissionItem;

/**
 * Request to change permissions on a type
 */
@XmlRootElement
public class TypePermissionRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private List<TypePermissionItem> perms;

	private List<RoleItem> roles;

	/**
	 * Set permissions also to the subgroups
	 */
	protected boolean subGroups;

	/**
	 * Set permissions also to the subobjects
	 */
	protected boolean subObjects;

	/**
	 * List of permissions to change
	 * @return permission list
	 */
	public List<TypePermissionItem> getPerms() {
		return perms;
	}

	/**
	 * Set permission list
	 * @param perms list
	 * @return fluent API
	 */
	public TypePermissionRequest setPerms(List<TypePermissionItem> perms) {
		this.perms = perms;
		return this;
	}

	/**
	 * List of role assignments to change
	 * @return role list
	 */
	public List<RoleItem> getRoles() {
		return roles;
	}

	/**
	 * Set role list
	 * @param roles list
	 * @return fluent API
	 */
	public TypePermissionRequest setRoles(List<RoleItem> roles) {
		this.roles = roles;
		return this;
	}

	/**
	 * True to set permissions also to subgroups, false for only the given group
	 * @return true for subgroups, false if not
	 */
	public boolean isSubGroups() {
		return subGroups;
	}

	/**
	 * Set whether to set permissions on the subgroups
	 * @param subGroups true for subgroups, false if not
	 * @return fluent API
	 */
	public TypePermissionRequest setSubGroups(boolean subGroups) {
		this.subGroups = subGroups;
		return this;
	}

	/**
	 * True to set permissions also for subobjects, false for only the given object
	 * @return true for subobjects, false if not
	 */
	public boolean isSubObjects() {
		return subObjects;
	}

	/**
	 * Set whether to set permissions for the subobjects
	 * @param subObjects true for subobjects, false if not
	 * @return fluent API
	 */
	public TypePermissionRequest setSubObjects(boolean subObjects) {
		this.subObjects = subObjects;
		return this;
	}
}
