package com.gentics.contentnode.staging;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Node / Object ID pair for the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedNodeIdObjectId implements Serializable {

	private static final long serialVersionUID = 5613655886773999023L;

	/**
	 * Node ID
	 */
	private String nodeId;

	/**
	 * Object ID
	 */
	private String objectId;

	/**
	 * Get node UUID.
	 * 
	 * @return
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * Set node UUID.
	 * 
	 * @param nodeId
	 */
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get object UUID.
	 * 
	 * @return
	 */
	public String getObjectId() {
		return objectId;
	}

	/**
	 * Set object UUID.
	 * 
	 * @param objectId
	 */
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
}
