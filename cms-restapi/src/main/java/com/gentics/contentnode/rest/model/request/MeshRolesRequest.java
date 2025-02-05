package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to set Mesh roles for a Mesh CR
 */
@XmlRootElement
public class MeshRolesRequest implements Serializable {
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
	public MeshRolesRequest setRoles(List<String> roles) {
		this.roles = roles;
		return this;
	}
}
