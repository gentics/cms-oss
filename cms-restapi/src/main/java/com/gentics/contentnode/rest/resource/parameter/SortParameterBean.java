package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

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
	public String sort = "name";

	public SortParameterBean setSort(String sort) {
		this.sort = sort;
		return this;
	}
}
