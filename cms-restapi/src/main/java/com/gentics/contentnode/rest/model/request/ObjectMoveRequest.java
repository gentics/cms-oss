package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request object for moving a single object
 */
@XmlRootElement
public class ObjectMoveRequest {
	/**
	 * Target folder ID
	 */
	private int folderId;

	/**
	 * Target node ID
	 */
	private Integer nodeId;

	/**
	 * True if all languages shall be moved (for pages), false if only the given pages. Default is true
	 */
	private Boolean allLanguages;

	/**
	 * Create empty instance
	 */
	public ObjectMoveRequest() {
	}

	/**
	 * Target folder ID
	 * @return target folder ID
	 */
	public int getFolderId() {
		return folderId;
	}

	/**
	 * Set the target folder ID
	 * @param folderId target folder ID
	 */
	public void setFolderId(int folderId) {
		this.folderId = folderId;
	}

	/**
	 * Target node ID for moving into a channel (may be null)
	 * @return target node ID
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Set the target node ID
	 * @param nodeId target node ID
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * True if all languages shall be moved (for pages), false if only the given pages. Default is true
	 * @return true for all languages
	 */
	public Boolean isAllLanguages() {
		return allLanguages;
	}

	/**
	 * Set whether all languages of pages shall be moved
	 * @param allLanguages true for all languages
	 */
	public void setAllLanguages(Boolean allLanguages) {
		this.allLanguages = allLanguages;
	}
}
