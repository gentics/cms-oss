package com.gentics.contentnode.rest.model.perm;

import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Permissions map
 */
@XmlRootElement
public class PermissionsMap {
	protected Map<PermType, Boolean> permissions;

	protected RolePermissions rolePermissions;

	/**
	 * Create an instance
	 */
	public PermissionsMap() {
	}

	/**
	 * Permissions not specific to languages
	 * @return permissions map
	 */
	public Map<PermType, Boolean> getPermissions() {
		return permissions;
	}

	/**
	 * Set the permissions map
	 * @param permissions permissions map
	 */
	public void setPermissions(Map<PermType, Boolean> permissions) {
		this.permissions = permissions;
	}

	/**
	 * Role specific permissions
	 * @return role permissions
	 */
	public RolePermissions getRolePermissions() {
		return rolePermissions;
	}

	/**
	 * Set the role specific permissions
	 * @param rolePermissions role specific permissions
	 */
	public void setRolePermissions(RolePermissions rolePermissions) {
		this.rolePermissions = rolePermissions;
	}
}
