package com.gentics.contentnode.rest.resource.parameter;

import java.util.List;

import com.gentics.contentnode.rest.model.request.Permission;

import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for permissions filtering.
 */
public class PermsFilterParameterBean {

	/**
	 * Query string for permissions filter
	 */
	@QueryParam("permitted")
	public List<Permission> permitted;

	public PermsFilterParameterBean setPermitted(List<Permission> permitted) {
		this.permitted = permitted;
		return this;
	}
}
