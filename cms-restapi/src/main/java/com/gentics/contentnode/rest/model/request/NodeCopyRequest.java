package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request to copy a node
 */
@XmlRootElement
public class NodeCopyRequest {
	private boolean pages = false;

	private boolean templates = false;

	private boolean files = false;

	private boolean workflows = false;

	private int copies = 1;

	/**
	 * True to copy pages
	 * @return pages flag
	 */
	public boolean isPages() {
		return pages;
	}

	/**
	 * Set pages flag
	 * @param pages flag
	 * @return fluent API
	 */
	public NodeCopyRequest setPages(boolean pages) {
		this.pages = pages;
		return this;
	}

	/**
	 * True to copy templates
	 * @return templates flag
	 */
	public boolean isTemplates() {
		return templates;
	}

	/**
	 * Set templates flag
	 * @param templates flag
	 * @return fluent API
	 */
	public NodeCopyRequest setTemplates(boolean templates) {
		this.templates = templates;
		return this;
	}

	/**
	 * True to copy files
	 * @return files flag
	 */
	public boolean isFiles() {
		return files;
	}

	/**
	 * Set files flag
	 * @param files flag
	 * @return fluent API
	 */
	public NodeCopyRequest setFiles(boolean files) {
		this.files = files;
		return this;
	}

	/**
	 * True to copy workflows
	 * @return workflows flag
	 */
	public boolean isWorkflows() {
		return workflows;
	}

	/**
	 * Set workflows flag
	 * @param workflows flag
	 * @return fluent API
	 */
	public NodeCopyRequest setWorkflows(boolean workflows) {
		this.workflows = workflows;
		return this;
	}

	/**
	 * Number of copies
	 * @return number
	 */
	public int getCopies() {
		return copies;
	}

	/**
	 * Set number of copies
	 * @param copies number
	 * @return fluent API
	 */
	public NodeCopyRequest setCopies(int copies) {
		this.copies = copies;
		return this;
	}
}
