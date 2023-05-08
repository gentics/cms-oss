package com.gentics.contentnode.rest.model.perm;

import java.io.Serializable;
import java.util.List;

/**
 * Permissions set on a type or instance
 */
public class TypePermissions implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3659408489155311945L;

	private String type;

	private Integer id;

	private Integer channelId;

	private String label;

	private String description;

	private List<TypePermissionItem> perms;

	private List<RoleItem> roles;

	/**
	 * Flag whether the type/instance has children
	 */
	protected boolean children;

	/**
	 * Flag to mark editable
	 */
	protected boolean editable;

	/**
	 * Type
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set type
	 * @param type type
	 * @return fluent API
	 */
	public TypePermissions setType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * Optional instance ID
	 * @return instance ID (may be null)
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set instance ID
	 * @param id ID
	 * @return fluent API
	 */
	public TypePermissions setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Optional channel ID for folders
	 * @return channel ID (may be null)
	 */
	public Integer getChannelId() {
		return channelId;
	}

	/**
	 * Set channel ID
	 * @param channelId channel ID
	 * @return fluent API
	 */
	public TypePermissions setChannelId(Integer channelId) {
		this.channelId = channelId;
		return this;
	}

	/**
	 * Label of the type/instance
	 * @return label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Set label
	 * @param label label
	 * @return fluent API
	 */
	public TypePermissions setLabel(String label) {
		this.label = label;
		return this;
	}

	/**
	 * Description
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set description
	 * @param description description
	 * @return fluent API
	 */
	public TypePermissions setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * List of permissions available for the type
	 * @return permission list
	 */
	public List<TypePermissionItem> getPerms() {
		return perms;
	}

	/**
	 * Set permissions
	 * @param perms permission list
	 * @return fluent API
	 */
	public TypePermissions setPerms(List<TypePermissionItem> perms) {
		this.perms = perms;
		return this;
	}

	/**
	 * List of available roles
	 * @return list of roles
	 */
	public List<RoleItem> getRoles() {
		return roles;
	}

	/**
	 * Set roles
	 * @param roles role list
	 * @return fluent API
	 */
	public TypePermissions setRoles(List<RoleItem> roles) {
		this.roles = roles;
		return this;
	}

	/**
	 * True if the type/instance has children
	 * @return flag
	 */
	public boolean isChildren() {
		return children;
	}

	/**
	 * Set child flag
	 * @param children flag
	 * @return fluent API
	 */
	public TypePermissions setChildren(boolean children) {
		this.children = children;
		return this;
	}

	/**
	 * True if the permissions can be changed by the current user
	 * @return editable flag
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * Set editable flag
	 * @param editable flag
	 * @return fluent API
	 */
	public TypePermissions setEditable(boolean editable) {
		this.editable = editable;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		string.append(type);
		if (id != null) {
			string.append(" (").append(id).append(")");
		}
		if (label != null) {
			string.append(" ").append(label);
		}
		if (channelId != null) {
			string.append(String.format(" channel: %d", channelId));
		}
		string.append(String.format(" children: %b", children));
		string.append(String.format(" editable: %b", editable));
		return string.toString();
	}
}
