/*
 * @author norbert
 * @date 14.10.2010
 * @version $Id: FolderListResponse.java,v 1.1.6.2 2011-03-18 09:45:33 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Folder;

/**
 * @author norbert
 *
 */
@XmlRootElement
public class LegacyFolderListResponse extends StagingResponse<String> {

	private static final long serialVersionUID = -407622727941885997L;

	/**
	 * list of folders
	 */
	private List<Folder> folders;

	/**
	 * True if more items are available (paging)
	 */
	private Boolean hasMoreItems;

	/**
	 * Total number of items present (paging)
	 */
	private Integer numItems;

	/**
	 * List of folder ids (or [nodeId/folderId]), which do not exist on the
	 * backend (at least not visibly for the user), but were requested to be
	 * "opened"
	 */
	private List<String> deleted;

	/**
	 * Empty constructor
	 */
	public LegacyFolderListResponse() {}

	public LegacyFolderListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Get the list of folders
	 * @return list of folders
	 */
	public List<Folder> getFolders() {
		return folders;
	}

	/**
	 * True if more items are available (paging)
	 * @return true if more items are present
	 */
	public Boolean isHasMoreItems() {
		return hasMoreItems;
	}

	/**
	 * Get total number of items present
	 * @return total number of items present
	 */
	public Integer getNumItems() {
		return numItems;
	}

	/**
	 * Set the list of folders
	 * @param folders list of folders
	 */
	public void setFolders(List<Folder> folders) {
		this.folders = folders;
	}

	/**
	 * Set true when more items are available
	 * @param hasMoreItems true if more items are available
	 */
	public void setHasMoreItems(Boolean hasMoreItems) {
		this.hasMoreItems = hasMoreItems;
	}

	/**
	 * Set the total number of items present
	 * @param numItems total number of items present
	 */
	public void setNumItems(Integer numItems) {
		this.numItems = numItems;
	}

	/**
	 * List of folderIds (or [nodeId/folderId]s), which were requested to be opened (when getting folder structures),
	 * but do not exist in the backend (at least not visible for the user)
	 * @return list of folder ids
	 */
	public List<String> getDeleted() {
		return deleted;
	}

	/**
	 * Set the list of folder ids, that do not exist in the backend
	 * @param deleted list of folder ids
	 */
	public void setDeleted(List<String> deleted) {
		this.deleted = deleted;
	}
}
