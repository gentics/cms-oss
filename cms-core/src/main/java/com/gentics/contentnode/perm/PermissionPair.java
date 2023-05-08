package com.gentics.contentnode.perm;

import java.util.Objects;

/**
 * Class encapsulating group and role permission bits
 */
public class PermissionPair {
	/**
	 * Group Permissions
	 */
	protected Permissions groupPermissions;

	/**
	 * Role Permissions
	 */
	protected Permissions rolePermissions;

	/**
	 * Create an empty instance
	 */
	public PermissionPair() {
	}

	/**
	 * Create instance with group permissions
	 * @param groupPermissions group permissions
	 */
	public PermissionPair(Permissions groupPermissions) {
		this.groupPermissions = groupPermissions;
	}

	/**
	 * Create instance with group and role permissions
	 * @param groupPermissions group permissions
	 * @param rolePermissions role permissions
	 */
	public PermissionPair(Permissions groupPermissions, Permissions rolePermissions) {
		this.groupPermissions = groupPermissions;
		this.rolePermissions = rolePermissions;
	}

	/**
	 * Get the group permissions
	 * @return group permissions
	 */
	public Permissions getGroupPermissions() {
		return groupPermissions;
	}

	/**
	 * Set the group permission bits
	 * @param groupPermissions group permission bits
	 */
	public void setGroupPermissions(Permissions groupPermissions) {
		this.groupPermissions = groupPermissions;
	}

	/**
	 * Get the role permissions
	 * @return role permissions
	 */
	public Permissions getRolePermissions() {
		return rolePermissions;
	}

	/**
	 * Set the role permission bits
	 * @param rolePermissions role permission bits
	 */
	public void setRolePermissions(Permissions rolePermissions) {
		this.rolePermissions = rolePermissions;
	}

	/**
	 * Check the given permission bits.
	 * @param groupBit group bit
	 * @param roleBit role bit
	 * @return true if either the group bit or the role bit is set, false if neither is set
	 */
	public boolean checkPermissionBits(int groupBit, int roleBit) {
		if (groupBit >= 0 && groupPermissions != null && groupPermissions.check(groupBit)) {
			return true;
		} else if (roleBit >= 0 && rolePermissions != null && rolePermissions.check(roleBit)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PermissionPair) {
			PermissionPair other = (PermissionPair) obj;
			return Objects.equals(groupPermissions, other.groupPermissions) && Objects.equals(rolePermissions, other.rolePermissions);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		if (groupPermissions != null) {
			str.append(groupPermissions);
		} else {
			str.append("-");
		}
		str.append("|");
		if (rolePermissions != null) {
			str.append(rolePermissions);
		} else {
			str.append("-");
		}

		return str.toString();
	}
}
