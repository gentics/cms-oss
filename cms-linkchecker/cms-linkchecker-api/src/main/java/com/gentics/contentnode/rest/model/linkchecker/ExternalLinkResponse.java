package com.gentics.contentnode.rest.model.linkchecker;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Response containing information about an external link
 */
@XmlRootElement
public class ExternalLinkResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 9008032075245485235L;

	private ExternalLink link;

	private int page;

	private int node;

	private int global;

	/**
	 * External link
	 * @return external link
	 */
	public ExternalLink getLink() {
		return link;
	}

	/**
	 * Set the external link
	 * @param link external link
	 */
	public void setLink(ExternalLink link) {
		this.link = link;
	}

	/**
	 * Number of occurrences of the URL in the page
	 * @documentationExample 1
	 * @return number
	 */
	public int getPage() {
		return page;
	}

	/**
	 * Set occurrences in page
	 * @param page number
	 */
	public void setPage(int page) {
		this.page = page;
	}

	/**
	 * Number of occurrences of the URL in the node
	 * @documentationExample 3
	 * @return number
	 */
	public int getNode() {
		return node;
	}

	/**
	 * Set occurrences in Node
	 * @param node number
	 */
	public void setNode(int node) {
		this.node = node;
	}

	/**
	 * Number of global occurrences of the URL
	 * @documentationExample 17
	 * @return number
	 */
	public int getGlobal() {
		return global;
	}

	/**
	 * Set global occurrences
	 * @param global number
	 */
	public void setGlobal(int global) {
		this.global = global;
	}
}
