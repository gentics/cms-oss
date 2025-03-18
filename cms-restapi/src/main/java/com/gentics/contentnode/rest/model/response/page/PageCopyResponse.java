package com.gentics.contentnode.rest.model.response.page;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response for a page copy request
 */
@XmlRootElement
public class PageCopyResponse extends GenericResponse {

	/**
	 * The copied pages
	 */
	private List<Page> pages = new ArrayList<Page>();

	/**
	 * List of source page Ids and their mapping to the target folders and new
	 * page ids.
	 */
	private List<PageCopyResultInfo> pageCopyMappings = new ArrayList<PageCopyResultInfo>();

	/**
	 * Empty constructor needed by JAXB
	 */
	public PageCopyResponse() {
	}

	/**
	 * Create a copy response
	 * 
	 * @param message
	 * @param responseInfo
	 */
	public PageCopyResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Build response
	 * 
	 * @param message
	 * @param responseInfo
	 * @param copiedPages
	 */
	public PageCopyResponse(Message message, ResponseInfo responseInfo, List<com.gentics.contentnode.rest.model.Page> copiedPages) {
		super(message, responseInfo);
		this.pages = copiedPages;
	}

	/**
	 * Return the list of copied pages
	 * 
	 * @return
	 */
	public List<Page> getPages() {
		return pages;
	}

	/**
	 * Set the list of copied pages
	 */
	public void setPages(List<Page> pages) {
		this.pages = pages;
	}

	/**
	 * Return the mappings for copied pages. The mapping contains information
	 * which source page was copied to which folders.
	 * 
	 * @return
	 */
	public List<PageCopyResultInfo> getPageCopyMappings() {
		return pageCopyMappings;
	}

	/**
	 * Set the list of page copy mappings.
	 * 
	 * @param pageCopyMappings
	 */
	public void setPageCopyMappings(List<PageCopyResultInfo> pageCopyMappings) {
		this.pageCopyMappings = pageCopyMappings;
	}
}
