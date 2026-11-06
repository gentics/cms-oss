package com.gentics.contentnode.cluster;

import java.io.Serializable;

import com.gentics.contentnode.perm.PermissionStore;

/**
 * Cluster message for {@link PermissionStore} synchronization
 */
public class PermSyncMessage implements Serializable {
	/**
	 * Serial Version UId
	 */
	private static final long serialVersionUID = 109155159529522319L;

	/**
	 * Message Type
	 */
	protected PermSyncMessageType type;

	/**
	 * Group ID
	 */
	protected int groupId;

	/**
	 * Role ID
	 */
	protected int roleId;

	/**
	 * Object Type
	 */
	protected int objectType;

	/**
	 * Object ID
	 */
	protected int objectId;

	/**
	 * Get type
	 * @return type
	 */
	public PermSyncMessageType getType() {
		return type;
	}

	/**
	 * Set message type
	 * @param type type
	 * @return fluent API
	 */
	public PermSyncMessage setType(PermSyncMessageType type) {
		this.type = type;
		return this;
	}

	/**
	 * Get the group ID
	 * @return group ID
	 */
	public int getGroupId() {
		return groupId;
	}

	/**
	 * Set the group ID
	 * @param groupId group ID
	 * @return fluent API
	 */
	public PermSyncMessage setGroupId(int groupId) {
		this.groupId = groupId;
		return this;
	}

	/**
	 * Get the role ID
	 * @return role ID
	 */
	public int getRoleId() {
		return roleId;
	}

	/**
	 * Set the role ID
	 * @param roleId role ID
	 * @return fluent API
	 */
	public PermSyncMessage setRoleId(int roleId) {
		this.roleId = roleId;
		return this;
	}

	/**
	 * Get the object type
	 * @return object type
	 */
	public int getObjectType() {
		return objectType;
	}

	/**
	 * Set the object type
	 * @param objectType object type
	 * @return fluent API
	 */
	public PermSyncMessage setObjectType(int objectType) {
		this.objectType = objectType;
		return this;
	}

	/**
	 * Get the object ID
	 * @return object ID
	 */
	public int getObjectId() {
		return objectId;
	}

	/**
	 * Set the object ID
	 * @param objectId object ID
	 * @return fluent API
	 */
	public PermSyncMessage setObjectId(int objectId) {
		this.objectId = objectId;
		return this;
	}

	/**
	 * Execute the handler for the message
	 */
	void execute() {
		if (type != null) {
			type.execute(this);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("PermSyncMessage type: %s", type));
		if (groupId != 0) {
			builder.append(String.format(", groupId: %d", groupId));
		}
		if (roleId != 0) {
			builder.append(String.format(", roleId: %d", roleId));
		}
		if (objectType != 0) {
			builder.append(String.format(", objectType: %d", objectType));
		}
		if (objectId != 0) {
			builder.append(String.format(", objectId: %d", objectId));
		}
		return builder.toString();
	}
}
