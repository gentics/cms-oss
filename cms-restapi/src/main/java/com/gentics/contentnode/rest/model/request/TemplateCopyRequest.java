package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to create a copy of a template
 */
@XmlRootElement
public class TemplateCopyRequest {
	private int folderId;

	private Integer nodeId;

	/**
	 * Folder ID, where the copy shall be created
	 * @return folder ID
	 */
	public int getFolderId() {
		return folderId;
	}

	/**
	 * Set the folder ID
	 * @param folderId folder ID
	 * @return fluent API
	 */
	public TemplateCopyRequest setFolderId(int folderId) {
		this.folderId = folderId;
		return this;
	}

	/**
	 * Optional node ID to create the copy in a channel
	 * @return node ID
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID
	 * @param nodeId node ID
	 * @return fluent API
	 */
	public TemplateCopyRequest setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
		return this;
	}
}
