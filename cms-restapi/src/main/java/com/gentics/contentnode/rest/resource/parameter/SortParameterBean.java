package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.mcp.McpToolParam;

/**
 * Parameter bean for sort parameters
 */
public class SortParameterBean {
	/**
	 * Comma separated list of sorted attributes.
	 * Each attribute name may be prefixed with <code>+</code> for sorting in ascending order or <code>-</code> for sorting in descending order
	 */
	@QueryParam("sort")
	@DefaultValue("name")
	@McpToolParam(description = "Comma separated list of attributes to sort by. Prefix an attribute with + for "
			+ "ascending order or - for descending order.", required = false)
	public String sort = "name";

	public SortParameterBean setSort(String sort) {
		this.sort = sort;
		return this;
	}
}
