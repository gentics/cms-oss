package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request object for linking a template to folders or unlinking a template from folders
 */
@XmlRootElement
public class LinkRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2429084913468665240L;

	/**
	 * Flag to link/unlink recursively
	 */
	@DefaultValue("false")
	protected boolean recursive = false;

	/**
	 * Node ID
	 */
	protected Integer nodeId;

	/**
	 * Set of folder IDs (may be globalIds)
	 */
	protected Set<String> folderIds = new HashSet<String>();

	/**
	 * Flag to delete the template(s) when unlinked from the last folder
	 */
	@DefaultValue("false")
	protected boolean delete = false;

	/**
	 * Create empty request
	 */
	public LinkRequest() {
	}

	/**
	 * True if the template shall be linked to or unlinked from the given folders recursively (including all subfolders).
	 * The default is false (not recursive)
	 * @return true for recursive, false if not
	 */
	public boolean isRecursive() {
		return recursive;
	}

	/**
	 * Set whether linking shall be done recursively
	 * @param recursive true for recursive, false if not
	 */
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	/**
	 * Set of folder IDs to handle. Folder IDs may either be local or global IDs.
	 * @return set of folder IDs
	 */
	public Set<String> getFolderIds() {
		return folderIds;
	}

	/**
	 * Set folder IDs to handle
	 * @param folderIds handled folder IDs
	 */
	public void setFolderIds(Set<String> folderIds) {
		this.folderIds = folderIds;
	}

	/**
	 * Node ID for handling channel local folders.
	 * Note that linking templates to folders is always done for the master objects. It is not possible to have a different linking for inherited or localized templates or folders.
	 * @return node ID
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Set the Node ID for linking to channel local folders
	 * @param nodeId node ID
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * True to delete the template, if it is unlinked from the last folder.
	 * @return true to delete
	 */
	public boolean isDelete() {
		return delete;
	}

	/**
	 * Set delete flag
	 * @param delete delete flag
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}
}
