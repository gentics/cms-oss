/*
 * @author Clemens
 * @date Oct 6, 2010
 * @version $Id: PageListResponse.java,v 1.2.4.2.2.1 2011-03-15 14:02:03 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Page;

/**
 * Response for a page list request
 * @author Clemens
 */
@XmlRootElement
public class LegacyPageListResponse extends StagingResponse<String> {
	private static final long serialVersionUID = 5798697357733469204L;

	private List<Page> pages;

	/**
	 * True if more items are available (paging)
	 */
	private Boolean hasMoreItems;

	/**
	 * Total number of items present (paging)
	 */
	private Integer numItems;

	/**
	 * Empty constructor needed by JAXB
	 */
	public LegacyPageListResponse() {}

	public LegacyPageListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	public List<Page> getPages() {
		return pages;
	}

	/**
	 * True if more items are available (paging)
	 * @return true if more items are present
	 */
	public Boolean isHasMoreItems() {
		return hasMoreItems;
	}

	/**
	 * Get total number of items present
	 * @return total number of items present
	 */
	public Integer getNumItems() {
		return numItems;
	}

	/**
	 * NOTE: pages won't be listed correctly until a setter is defined
	 * @param pages
	 */
	public void setPages(List<Page> pages) {
		this.pages = pages;
	}

	/**
	 * Set true when more items are available
	 * @param hasMoreItems true if more items are available
	 */
	public void setHasMoreItems(Boolean hasMoreItems) {
		this.hasMoreItems = hasMoreItems;
	}

	/**
	 * Set the total number of items present
	 * @param numItems total number of items present
	 */
	public void setNumItems(Integer numItems) {
		this.numItems = numItems;
	}
}
