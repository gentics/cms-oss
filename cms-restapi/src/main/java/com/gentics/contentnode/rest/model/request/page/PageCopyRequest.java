package com.gentics.contentnode.rest.model.request.page;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request for a batch copy call
 */
@XmlRootElement
public class PageCopyRequest {

	/**
	 * Flag that indicates whether to create a new page when an existing page
	 * with the same filename was found in the target folder. By default no copy
	 * will be created. In this case the found page id would be returned by the
	 * response.
	 */
	protected boolean createCopy = false;

	/**
	 * Node ID for the source pages
	 */
	protected Integer nodeId;

	/**
	 * List of pages id's that should be copied
	 */
	protected List<Integer> sourcePageIds = new ArrayList<Integer>();

	/**
	 * List of target folders in which the pages should be copied.
	 */
	protected List<TargetFolder> targetFolders = new ArrayList<TargetFolder>();

	/**
	 * The list of target folder in which the pages should be copied.
	 * 
	 * @return List of target folder entries
	 */
	public List<TargetFolder> getTargetFolders() {
		return targetFolders;
	}

	/**
	 * The list of source page id's that should be copied to the target folders.
	 * 
	 * @return List of source page id's
	 */
	public List<Integer> getSourcePageIds() {
		return sourcePageIds;
	}

	/**
	 * Set the list of source page id's.
	 * 
	 * @param sourcePageIds
	 */
	public void setSourcePageIds(List<Integer> sourcePageIds) {
		this.sourcePageIds = sourcePageIds;
	}

	/**
	 * Set the target folders into which the pages should be copied.
	 * 
	 * @param targetFolders
	 */
	public void setTargetFolders(List<TargetFolder> targetFolders) {
		this.targetFolders = targetFolders;
	}

	/**
	 * Whether new copies should be created in folders in which already pages
	 * with the same name reside.
	 * 
	 * @return true, when the create copy flag is set and new copies should be
	 *         created. Otherwise false.
	 */
	public boolean isCreateCopy() {
		return createCopy;
	}

	/**
	 * Set the create copy flag. A new page will be created in the target folder
	 * even when an existing page with the same name was found that folder. This
	 * applies for all target folders.
	 * 
	 * @param createCopy
	 */
	public void setCreateCopy(boolean createCopy) {
		this.createCopy = createCopy;
	}

	/**
	 * Node ID for the source pages. If this is set to a channel, the channel variant of the given page will be copied.
	 * @return node ID for the source pages
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID
	 * @param nodeId node ID
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}
}
