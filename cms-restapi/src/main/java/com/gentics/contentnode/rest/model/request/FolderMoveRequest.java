package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request object for a request to move a folder
 */
@XmlRootElement
public class FolderMoveRequest {
	private int folderId = 0;

	private int nodeId = 0;

	/**
	 * Number of seconds the job may run in foreground
	 */
	private Integer foregroundTime;

	/**
	 * ID of the target folder (where to move the folder)
	 * 
	 * @return ID of the target folder
	 */
	public int getFolderId() {
		return folderId;
	}

	/**
	 * Set the ID of the target folder
	 * 
	 * @param folderId
	 *            target folder ID
	 */
	public void setFolderId(int folderId) {
		this.folderId = folderId;
	}

	/**
	 * ID of the target channel, when moving the folder into a channel. When
	 * left empty, the folder is moved into the master node.
	 * 
	 * @return target channel ID
	 */
	public int getNodeId() {
		return nodeId;
	}

	/**
	 * Set the target channel ID
	 * 
	 * @param nodeId
	 *            target channel ID
	 */
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Number of seconds, this job may stay in foreground. If moving takes
	 * longer, the request will return with a response, and moving the folder
	 * will be continued in background
	 * 
	 * @return foreground time in seconds
	 */
	public Integer getForegroundTime() {
		return foregroundTime;
	}

	/**
	 * Set the foreground time
	 * 
	 * @param foregroundTime
	 *            foreground time in seconds
	 */
	public void setForegroundTime(Integer foregroundTime) {
		this.foregroundTime = foregroundTime;
	}
}
