package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

/**
 * Parameter bean for paging of legacy list endpoints.
 */
public class LegacyPagingParameterBean {

	/**
	 * number of items to be skipped, set to 0 for skipping no items
	 */
	@QueryParam("skipCount")
	@DefaultValue("0")
	public int skipCount = 0;

	/**
	 * maximum number of items to be returned, set to -1 for returning all items
	 */
	@QueryParam("maxItems")
	@DefaultValue("-1")
	public int maxItems = -1;

	public LegacyPagingParameterBean setSkipCount(int skipCount) {
		this.skipCount = skipCount;
		return this;
	}

	public LegacyPagingParameterBean setMaxItems(int maxItems) {
		this.maxItems = maxItems;
		return this;
	}

	/**
	 * Create the replacement for this legacy parameter bean.
	 * @return A paging parameter bean with the same paging settings
	 */
	public PagingParameterBean toPagingParameterBean() {
		return new PagingParameterBean()
			.setPage(skipCount / maxItems + 1)
			.setPageSize(maxItems);
	}
}
