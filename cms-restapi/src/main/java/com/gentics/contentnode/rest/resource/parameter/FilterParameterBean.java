package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for filter parameters
 */
public class FilterParameterBean {
	/**
	 * Query string for filtering
	 */
	@QueryParam("q")
	public String query;

	public FilterParameterBean setQuery(String query) {
		this.query = query;
		return this;
	}
}
