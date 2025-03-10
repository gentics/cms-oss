package com.gentics.contentnode.rest.resource.parameter;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.request.WastebinSearch;

/**
 * Parameter bean for wastebin
 */
public class WastebinParameterBean {
	/**
	 * "exclude" (default) to exclude deleted items, "include" to include deleted
	 * items, "only" to return only deleted items
	 */
	@QueryParam("wastebin")
	@DefaultValue("exclude")
	public WastebinSearch wastebinSearch = WastebinSearch.exclude;

	public WastebinParameterBean setWastebinSearch(WastebinSearch wastebinSearch) {
		this.wastebinSearch = wastebinSearch;
		return this;
	}

	/**
	 * Create a clone of this parameter bean.
	 * @return A clone of this parameter bean
	 */
	public WastebinParameterBean clone() {
		return new WastebinParameterBean().setWastebinSearch(wastebinSearch);
	}
}
