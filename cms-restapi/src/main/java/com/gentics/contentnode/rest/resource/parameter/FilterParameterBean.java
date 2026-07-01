package com.gentics.contentnode.rest.resource.parameter;

import java.util.List;

import com.gentics.contentnode.rest.model.request.Permission;

import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for entity and/or permissions filter parameters
 */
public class FilterParameterBean {
	/**
	 * Query string for filtering
	 */
	@QueryParam("q")
	public String query;

	/**
	 * Query string for permissions filter
	 */
	@QueryParam("permitted")
	public List<Permission> permitted;

	public FilterParameterBean setQuery(String query) {
		this.query = query;
		return this;
	}

	public FilterParameterBean setPermitted(List<Permission> permitted) {
		this.permitted = permitted;
		return this;
	}
}
