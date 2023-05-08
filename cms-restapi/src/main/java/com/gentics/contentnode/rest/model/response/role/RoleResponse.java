package com.gentics.contentnode.rest.model.response.role;

import com.gentics.contentnode.rest.model.RoleModel;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response containing a role
 */
public class RoleResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4219288742864587222L;

	/**
	 * Role
	 */
	private RoleModel role;

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public RoleResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with message, response info and role
	 * @param message message
	 * @param responseInfo response info
	 * @param role role
	 */
	public RoleResponse(Message message, ResponseInfo responseInfo, RoleModel role) {
		super(message, responseInfo);
		setRole(role);
	}

	/**
	 * Role
	 * @return role
	 */
	public RoleModel getRole() {
		return role;
	}

	/**
	 * Set the role
	 * @param role role
	 * @return fluent API
	 */
	public RoleResponse setRole(RoleModel role) {
		this.role = role;
		return this;
	}
}
