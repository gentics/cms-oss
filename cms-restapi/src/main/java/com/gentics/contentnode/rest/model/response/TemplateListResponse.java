/*
 * @author norbert
 * @date 08.02.2011
 * @version $Id: TemplateListResponse.java,v 1.1.2.1.2.1 2011-03-15 14:02:03 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Template;

/**
 * Response for a template list request
 * @author norbert
 */
@XmlRootElement
public class TemplateListResponse extends GenericResponse {

	/**
	 * list of templates
	 */
	private List<Template> templates;

	/**
	 * True if more items are available (paging)
	 */
	private Boolean hasMoreItems;

	/**
	 * Total number of items present (paging)
	 */
	private Integer numItems;

	/**
	 * Empty constructor
	 */
	public TemplateListResponse() {}

	public TemplateListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Get the list of templates
	 * @return list of templates
	 */
	public List<Template> getTemplates() {
		return templates;
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
	 * Set the list of templates
	 * @param templates list of templates
	 */
	public void setTemplates(List<Template> templates) {
		this.templates = templates;
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
