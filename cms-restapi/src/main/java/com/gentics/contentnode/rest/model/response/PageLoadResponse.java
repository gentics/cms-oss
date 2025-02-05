/*
 * @author floriangutmann
 * @date Apr 6, 2010
 * @version $Id: PageLoadResponse.java,v 1.2 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Page;

/**
 * Response for a page load request.
 * 
 * @author floriangutmann
 */
@XmlRootElement
public class PageLoadResponse extends GenericResponse {

	private static final long serialVersionUID = -5653638510449412833L;
	/**
	 * Page object that contains the loaded page
	 */
	private Page page;

	/**
	 * Staging package inclusion status
	 */
	private StagingStatus stagingStatus;

	/**
	 * Empty constructor needed by JAXB
	 */
	public PageLoadResponse() {}

	/**
	 * Creates a PageLoadResponse with the provided single message and ResponseInfo.
	 * 
	 * @param message The messages that should be displayed to the user
	 * @param response ResponseInfo with the status of the response
	 */
	public PageLoadResponse(Message message, ResponseInfo responseInfo, Page page) {
		super(message, responseInfo);
		this.page = page;
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public StagingStatus getStagingStatus() {
		return stagingStatus;
	}

	public void setStagingStatus(StagingStatus stagingStatus) {
		this.stagingStatus = stagingStatus;
	}
}
