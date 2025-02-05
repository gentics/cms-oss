package com.gentics.contentnode.rest.model.response.role;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.RolePermissionsModel;
import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Response containing role permissions
 */
@XmlRootElement
public class RolePermResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2671945219854286938L;

	protected RolePermissionsModel perm;

	/**
	 * Role Permissions
	 * @return role perms
	 */
	public RolePermissionsModel getPerm() {
		return perm;
	}

	/**
	 * Set Role Permissions
	 * @param perm role permissions
	 * @return fluent API
	 */
	public RolePermResponse setPerm(RolePermissionsModel perm) {
		this.perm = perm;
		return this;
	}
}
