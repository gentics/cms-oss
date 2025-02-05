package com.gentics.contentnode.rest.model.response;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * This class contains the external links of a page.
 * 
 * @author Pedro Najor Cruz  Cruz
 */
@XmlRootElement
public class PageExternalLink implements Serializable {
	
	private static final long serialVersionUID = 8290039499262034596L;

	private Integer pageId;
	private String pageName;
	private List<String> links;

	/**
	 * Empty constructor
	 */
	public PageExternalLink() {

	}

	/**
	 * @param pageId
	 * @param pageName
	 * @param links
	 */
	public PageExternalLink(Integer pageId, String pageName, List<String> links) {
		super();
		this.pageId = pageId;
		this.pageName = pageName;
		this.links = links;
	}

	/**
	 * Get pageId
	 * @return the pageId
	 */
	public Integer getPageId() {
		return pageId;
	}
	/**
	 * Set pageId
	 * @param pageId the pageId to set
	 */
	public void setPageId(Integer pageId) {
		this.pageId = pageId;
	}
	/**
	 * Get pageName
	 * @return the pageName
	 */
	public String getPageName() {
		return pageName;
	}

	/**
	 * Set pageName
	 * @param pageName the pageName to set
	 */
	public void setPageName(String pageName) {
		this.pageName = pageName;
	}
	/**
	 * Get links
	 * @return the links
	 */
	public List<String> getLinks() {
		return links;
	}
	/**
	 * Set links
	 * @param links the links to set
	 */
	public void setLinks(List<String> links) {
		this.links = links;
	}
}
