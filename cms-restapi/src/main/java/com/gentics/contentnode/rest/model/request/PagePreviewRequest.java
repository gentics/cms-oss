/*
 * @author norbert
 * @date 07.03.2011
 * @version $Id: PagePreviewRequest.java,v 1.1.2.1 2011-03-07 16:37:17 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Page;

/**
 * Request for preview of a page
 * @author norbert
 */
@XmlRootElement
public class PagePreviewRequest {

	/**
	 * Page in the page save request
	 */
	private Page page;
	
	private Integer nodeId;

	/**
	 * Constructor used by JAXB
	 */
	public PagePreviewRequest() {}

	/**
	 * Creates a new PageSaveRequest with a specified page
	 * @param page The page to save
	 */
	public PagePreviewRequest(Page page) {
		this.page = page;
	}

	/**
	 * Get the page
	 * @return page
	 */
	public Page getPage() {
		return page;
	}

	/**
	 * Set the page
	 * @param page
	 */
	public void setPage(Page page) {
		this.page = page;
	}

	/**
	 * Get nodeId
	 * @return the nodeId
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Set nodeId
	 * @param nodeId the nodeId to set
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}
}
