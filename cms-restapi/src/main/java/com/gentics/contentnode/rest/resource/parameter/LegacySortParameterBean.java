package com.gentics.contentnode.rest.resource.parameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

/**
 * Parameter bean for sorting of legacy list endpoints.
 */
public class LegacySortParameterBean {

	/**
	 * (optional) attribute to sort by. It is possible to sort by name, cdate,
	 * edate, pdate, filename, template, folder, masterNode, priority, excluded,
	 * deletedat. defaults to name
	 */
	@QueryParam("sortby")
	@DefaultValue("name")
	public String sortBy = "name";

	/**
	 * (optional) result sort order - may be "asc" for ascending or "desc" for
	 * descending other strings will be ignored. defaults to "asc".
	 */
	@QueryParam("sortorder")
	@DefaultValue("asc")
	public String sortOrder = "asc";

	public LegacySortParameterBean setSortBy(String sortBy) {
		this.sortBy = sortBy;
		return this;
	}

	public LegacySortParameterBean setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
		return this;
	}

	/**
	 * Create the replacement for this legacy parameter bean.
	 * @return A sort parameter bean with the same sort criteria and sort order
	 */
	public SortParameterBean toSortParameterBean() {
		return new SortParameterBean()
			.setSort((sortOrder.equalsIgnoreCase("asc") ? "+" : "-") + sortBy);
	}
}
