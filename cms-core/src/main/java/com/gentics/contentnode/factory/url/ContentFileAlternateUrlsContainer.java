package com.gentics.contentnode.factory.url;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;

/**
 * Alternate URLs container for files
 */
public class ContentFileAlternateUrlsContainer extends AlternateUrlsContainer {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 9117885136258823558L;

	/**
	 * Create instance
	 * @param parent parent object
	 * @throws NodeException
	 */
	public ContentFileAlternateUrlsContainer(NodeObjectWithAlternateUrls parent) throws NodeException {
		super(parent);
	}

	@Override
	public String getTableName() {
		return "contentfile_alt_url";
	}

	@Override
	public String getReferenceColumnName() {
		return "contentfile_id";
	}
}
