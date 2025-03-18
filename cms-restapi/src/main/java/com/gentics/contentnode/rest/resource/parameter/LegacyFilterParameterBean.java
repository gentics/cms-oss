package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for filters of legacy list endpoints.
 */
public class LegacyFilterParameterBean {

	/**
	 * (optional) string to search files for - this will filter the results if
	 * either the ID, the name (partial match or the description (partial match)
	 * matches the given search string.
	 */
	@QueryParam("search")
	public String search;

	public LegacyFilterParameterBean setSearch(String search) {
		this.search = search;
		return this;
	}

	/**
	 * Create the replacement for this legacy parameter bean.
	 * @return A filter parameter bean with the same search query
	 */
	public FilterParameterBean toFilterParameterBean() {
		return new FilterParameterBean().setQuery(search);
	}
}
