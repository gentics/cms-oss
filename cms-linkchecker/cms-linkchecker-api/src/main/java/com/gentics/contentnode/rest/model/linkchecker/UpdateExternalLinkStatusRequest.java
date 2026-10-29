package com.gentics.contentnode.rest.model.linkchecker;

import java.util.List;

/**
 * Request to update the status of external links in a page
 */
public class UpdateExternalLinkStatusRequest {
	private List<ExternalLink> items;

	/**
	 * External links for which the status shall be updated
	 * @return list of links
	 */
	public List<ExternalLink> getItems() {
		return items;
	}

	/**
	 * Set link list
	 * @param items list
	 * @return fluent API
	 */
	public UpdateExternalLinkStatusRequest setItems(List<ExternalLink> items) {
		this.items = items;
		return this;
	}
}
