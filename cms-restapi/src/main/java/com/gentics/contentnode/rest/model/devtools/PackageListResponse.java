package com.gentics.contentnode.rest.model.devtools;

import java.util.List;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of packages
 */
public class PackageListResponse extends AbstractListResponse<Package> {
	/**
	 * Packages in the list
	 * @return list of packages
	 */
	public List<Package> getItems() {
		return super.getItems();
	}

	/**
	 * Set the packages
	 * @param items list of packages
	 */
	public void setItems(List<Package> items) {
		super.setItems(items);
	}
}
