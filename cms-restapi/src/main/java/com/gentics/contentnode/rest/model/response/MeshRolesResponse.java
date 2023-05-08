package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Response containing a list of Mesh role names
 */
@XmlRootElement
public class MeshRolesResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	protected List<String> roles;

	/**
	 * Mesh role names
	 * @return list of role names
	 */
	public List<String> getRoles() {
		return roles;
	}

	/**
	 * Set Mesh role names
	 * @param roles role names
	 * @return fluent API
	 */
	public MeshRolesResponse setRoles(List<String> roles) {
		this.roles = roles;
		return this;
	}
}
