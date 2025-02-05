package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.webcohesion.enunciate.metadata.DocumentationExample;

/**
 * Request to sanitize a folder publish directory
 */
@XmlRootElement
public class FolderPublishDirSanitizeRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3325186685987496099L;

	private int nodeId;

	private String publishDir;

	/**
	 * ID of the node/channel of the folder
	 * @return node ID
	 */
	@DocumentationExample("1")
	public int getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID
	 * @param nodeId node ID
	 * @return fluent API
	 */
	public FolderPublishDirSanitizeRequest setNodeId(int nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	/**
	 * Publish directory to be sanitized
	 * @return publish directory
	 */
	public String getPublishDir() {
		return publishDir;
	}

	/**
	 * Set publish directory
	 * @param publishDir publish directory
	 * @return fluent API
	 */
	public FolderPublishDirSanitizeRequest setPublishDir(String publishDir) {
		this.publishDir = publishDir;
		return this;
	}
}
