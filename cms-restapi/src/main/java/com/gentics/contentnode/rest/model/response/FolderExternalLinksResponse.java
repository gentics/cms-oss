package com.gentics.contentnode.rest.model.response;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * REST Model external links response. It contains the pages with external links.
 */
@XmlRootElement
public class FolderExternalLinksResponse extends GenericResponse implements Serializable {

	private static final long serialVersionUID = 4538567395398949128L;

	private List<PageExternalLink> pages;

	/**
	 * Empty constructor
	 */
	public FolderExternalLinksResponse() {

	}
	
	/**
	 * @param pages
	 */
	public FolderExternalLinksResponse(List<PageExternalLink> pages) {
		super();
		this.pages = pages;
	}

	/**
	 * @param message
	 * @param responseInfo
	 */
	public FolderExternalLinksResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Get pages
	 * @return the pages
	 */
	public List<PageExternalLink> getPages() {
		return pages;
	}

	/**
	 * Set pages
	 * @param pages the pages to set
	 */
	public void setPages(List<PageExternalLink> pages) {
		this.pages = pages;
	}
}
