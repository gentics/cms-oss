package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.mcp.McpToolParam;

/**
 * Parameter bean for entity filter parameters
 */
public class FilterParameterBean {
	/**
	 * Query string for filtering
	 */
	@QueryParam("q")
	@McpToolParam(description = "Query string for filtering.", required = false)
	public String query;

	public FilterParameterBean setQuery(String query) {
		this.query = query;
		return this;
	}

}
