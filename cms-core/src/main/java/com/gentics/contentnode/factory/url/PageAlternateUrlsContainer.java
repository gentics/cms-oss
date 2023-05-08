package com.gentics.contentnode.factory.url;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;

/**
 * Alternate URLs container for pages
 */
public class PageAlternateUrlsContainer extends AlternateUrlsContainer {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8986377674869149619L;

	/**
	 * Create instance
	 * @param parent parent object
	 * @throws NodeException
	 */
	public PageAlternateUrlsContainer(NodeObjectWithAlternateUrls parent) throws NodeException {
		super(parent);
	}

	@Override
	public String getTableName() {
		return "page_alt_url";
	}

	@Override
	public String getReferenceColumnName() {
		return "page_id";
	}
}
