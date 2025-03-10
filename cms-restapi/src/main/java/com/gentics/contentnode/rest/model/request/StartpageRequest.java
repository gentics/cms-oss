package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to set a folder startpage
 */
@XmlRootElement
public class StartpageRequest {

	/**
	 * Page Id to set as startpage
	 */
	private int pageId;

	/**
	 * Empty constructor
	 */
	public StartpageRequest() {}

	/**
	 * Get the startpage id
	 * @return startpage id
	 */
	public int getPageId() {
		return pageId;
	}

	/**
	 * Set the startpage id
	 * @param pageId startpage id
	 */
	public void setPageId(int pageId) {
		this.pageId = pageId;
	}
}
