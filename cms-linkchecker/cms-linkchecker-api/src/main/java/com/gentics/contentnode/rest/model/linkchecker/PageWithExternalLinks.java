package com.gentics.contentnode.rest.model.linkchecker;

import java.io.Serializable;
import java.util.Collection;

import com.gentics.contentnode.rest.model.Page;

/**
 * Model of a page with external links
 */
public class PageWithExternalLinks implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private Page page;

	private Collection<ExternalLink> links;

	private Boolean editable;

	/**
	 * Page containing the links
	 * @return page
	 */
	public Page getPage() {
		return page;
	}

	/**
	 * Set page
	 * @param page page
	 * @return fluent API
	 */
	public PageWithExternalLinks setPage(Page page) {
		this.page = page;
		return this;
	}

	/**
	 * External links contained in the page
	 * @return collection of links
	 */
	public Collection<ExternalLink> getLinks() {
		return links;
	}

	/**
	 * Set links
	 * @param links collection
	 * @return fluent API
	 */
	public PageWithExternalLinks setLinks(Collection<ExternalLink> links) {
		this.links = links;
		return this;
	}

	/**
	 * True when the page is editable by the user, false if not
	 * @return editable flag
	 */
	public Boolean getEditable() {
		return editable;
	}

	/**
	 * Set the editable flag
	 * @param editable flag
	 * @return fluent API
	 */
	public PageWithExternalLinks setEditable(Boolean editable) {
		this.editable = editable;
		return this;
	}

	@Override
	public String toString() {
		return String.format("Page: %d, Links: %s, Editable: %b", page.getId(), links, editable);
	}
}
