package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.perm.RoleItem;
import com.gentics.contentnode.rest.model.perm.TypePermissionItem;

/**
 * Response containing permissions and role assignments
 */
@XmlRootElement
public class TypePermissionResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Permissions
	 */
	private List<TypePermissionItem> perms;

	/**
	 * Role assignments
	 */
	private List<RoleItem> roles;

	/**
	 * Create empty instance
	 */
	public TypePermissionResponse() {
		super();
	}

	/**
	 * Create instance
	 * @param message message
	 * @param responseInfo response info
	 */
	public TypePermissionResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Permissions
	 * @return list of permissions
	 */
	public List<TypePermissionItem> getPerms() {
		return perms;
	}

	/**
	 * Set permissions
	 * @param perms permissions
	 * @return fluent API
	 */
	public TypePermissionResponse setPerms(List<TypePermissionItem> perms) {
		this.perms = perms;
		return this;
	}

	/**
	 * Roles
	 * @return list of roles
	 */
	public List<RoleItem> getRoles() {
		return roles;
	}

	/**
	 * Set roles
	 * @param roles roles
	 * @return fluent API
	 */
	public TypePermissionResponse setRoles(List<RoleItem> roles) {
		this.roles = roles;
		return this;
	}
}
