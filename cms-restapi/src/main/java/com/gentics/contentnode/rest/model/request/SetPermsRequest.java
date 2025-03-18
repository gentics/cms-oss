package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to set permission bits
 */
@XmlRootElement
public class SetPermsRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6039652722724839915L;

	/**
	 * Permission bits and roles
	 */
	protected String perm;

	/**
	 * ID of the group for which to set the permission
	 */
	protected int groupId;

	/**
	 * Set permissions also to the subgroups
	 */
	protected boolean subGroups;

	/**
	 * Set permissions also to the subobjects
	 */
	protected boolean subObjects;

	/**
	 * Set of roles that should be set. If null, no changes are made.
	 */
	protected Set<Integer> roleIds;

	/**
	 * Create an empty instance
	 */
	public SetPermsRequest() {
	}

	/**
	 * Permission bits and roles
	 * @return permission bits and roles
	 */
	public String getPerm() {
		return perm;
	}

	/**
	 * Set the permission bits and roles
	 * @param perm permission bits and roles
	 * @return fluent API
	 */
	public SetPermsRequest setPerm(String perm) {
		this.perm = perm;
		return this;
	}

	/**
	 * ID of the group
	 * @return ID of the group
	 */
	public int getGroupId() {
		return groupId;
	}

	/**
	 * Set the group ID
	 * @param groupId group ID
	 * @return fluent API
	 */
	public SetPermsRequest setGroupId(int groupId) {
		this.groupId = groupId;
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
	public SetPermsRequest setSubGroups(boolean subGroups) {
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
	public SetPermsRequest setSubObjects(boolean subObjects) {
		this.subObjects = subObjects;
		return this;
	}

	/**
	 * The role IDs of roles that should be set when the request is completed. All roles not mentioned are removed. If not set, no changes to the roles will be performed.
	 * @return the role ids
	 */
	public Set<Integer> getRoleIds() {
		return roleIds;
	}

	/**
	 * Set set of role IDs to be set
	 * @param roleIds Role-IDs to set or null if no change required
	 * @return fluent API
	 */
	public SetPermsRequest setRoleIds(Set<Integer> roleIds) {
		this.roleIds = roleIds;
		return this;
	}
}
