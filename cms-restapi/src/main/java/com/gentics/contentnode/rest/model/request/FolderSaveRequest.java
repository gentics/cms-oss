/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: FolderSaveRequest.java,v 1.1 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Folder;

/**
 * Request object for a request to save a folder
 * @author norbert
 */
@XmlRootElement
public class FolderSaveRequest {

	/**
	 * Folder to save
	 */
	private Folder folder;

	/**
	 * Node ID (for setting the publish directory recursively)
	 */
	private Integer nodeId;

	/**
	 * True to set the publish directory recursively
	 */
	private Boolean recursive;

	/**
	 * When true, saving the folder with duplicate name will fail, when false, the name will be made unique before saving
	 */
	private Boolean failOnDuplicate;

	/**
	 * List of object tags, that shall be passed on to subfolders as well
	 */
	private List<String> tagsToSubfolders;

	/**
	 * Number of seconds the job may run in foreground
	 */
	private Integer foregroundTime;

	/**
	 * Constructor used by JAXB
	 */
	public FolderSaveRequest() {}

	/**
	 * @return the folder
	 */
	public Folder getFolder() {
		return folder;
	}

	/**
	 * @param folder the folder to set
	 */
	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	/**
	 * Node ID, when setting the publish directory recursively.
	 * @return node ID
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * List of object tags, that shall be passed on to subfolders. May contain names (with or without the prefix 'object.') or IDs
	 * @return list of object tags
	 */
	public List<String> getTagsToSubfolders() {
		return tagsToSubfolders;
	}

	/**
	 * Set the node ID
	 * @param nodeId node ID
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * True to set the publish directory recursively, false (default) to only change this given folder
	 * @return true for setting recursively
	 */
	public Boolean getRecursive() {
		return recursive;
	}

	/**
	 * Set true to change the publish directory recursively
	 * @param recursive true to for setting recursively
	 */
	public void setRecursive(Boolean recursive) {
		this.recursive = recursive;
	}

	/**
	 * True if saving the folder with a duplicate name will fail. If false (default) the name will be made unique before saving
	 * @return true or false
	 */
	public Boolean getFailOnDuplicate() {
		return failOnDuplicate;
	}

	/**
	 * Set whether saving shall fail on duplicate names
	 * @param failOnDuplicate true to fail on duplicate names
	 */
	public void setFailOnDuplicate(Boolean failOnDuplicate) {
		this.failOnDuplicate = failOnDuplicate;
	}

	/**
	 * Set list of object tags to pass on to subfolders
	 * @param tagsToSubfolders list of object tags
	 */
	public void setTagsToSubfolders(List<String> tagsToSubfolders) {
		this.tagsToSubfolders = tagsToSubfolders;
	}

	/**
	 * Number of seconds, the job may run in the foreground
	 * @return foreground time in seconds
	 */
	public Integer getForegroundTime() {
		return foregroundTime;
	}

	/**
	 * Set the foreground time
	 * @param foregroundTime foreground time
	 */
	public void setForegroundTime(Integer foregroundTime) {
		this.foregroundTime = foregroundTime;
	}
}
