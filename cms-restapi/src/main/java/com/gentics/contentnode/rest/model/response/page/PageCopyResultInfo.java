package com.gentics.contentnode.rest.model.response.page;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The page copy result info contains information about each copy action that
 * has taken place during a page copy call. When a page is copied to multiple
 * folders multiple objects of this class will be added to the page copy
 * response.
 * 
 */
@XmlRootElement
public class PageCopyResultInfo {

	private Integer sourcePageId;
	private Integer newPageId;
	private Integer targetFolderId;
	private Integer targetFolderChannelId;

	/**
	 * Empty constructor needed by JAXB
	 */
	public PageCopyResultInfo() {
	}

	/**
	 * Returns the page id of the newly created copy
	 * 
	 * @return
	 */
	public Integer getNewPageId() {
		return newPageId;
	}

	/**
	 * Set the page id for the newly created copy
	 * 
	 * @param newPageId
	 */
	public void setNewPageId(Integer newPageId) {
		this.newPageId = newPageId;
	}

	/**
	 * Return the target folder id in which the page copy was created.
	 * 
	 * @return
	 */
	public Integer getTargetFolderId() {
		return targetFolderId;
	}

	/**
	 * Set the target folder id for the folder in which the copy was created.
	 * 
	 * @param folderTargetId
	 */
	public void setTargetFolderId(Integer targetFolderId) {
		this.targetFolderId = targetFolderId;
	}

	/**
	 * Returns the folder channel id for the parent folder of the page copy
	 * 
	 * @return
	 */
	public Integer getTargetFolderChannelId() {
		return targetFolderChannelId;
	}

	/**
	 * Set the channel id for the parent folder of the page copy
	 * 
	 * @param folderChannelId
	 */
	public void setTargetFolderChannelId(Integer targetFolderChannelId) {
		this.targetFolderChannelId = targetFolderChannelId;
	}

	/**
	 * Returns the source page id of the page that was used for creating the
	 * copy
	 * 
	 * @return
	 */
	public Integer getSourcePageId() {
		return sourcePageId;
	}

	/**
	 * Set the source page id of the page that was used for creating the copy
	 * 
	 * @param sourcePageId
	 */
	public void setSourcePageId(Integer sourcePageId) {
		this.sourcePageId = sourcePageId;
	}

}
