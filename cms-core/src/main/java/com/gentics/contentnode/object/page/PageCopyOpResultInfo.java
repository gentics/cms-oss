package com.gentics.contentnode.object.page;

import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;

/**
 * This class is used for generating page copy operation result information.
 * Each copy action would normally generate a single
 * {@link PageCopyOpResultInfo} instance. Each instance stores the information
 * which source page is used to create a copy in the given target folder.
 * 
 * @author johannes2
 * 
 */
public class PageCopyOpResultInfo {

	private Page sourcePage;
	private Page createdPageCopy;
	private Folder targetFolder;
	private Integer targetChannelId;

	/**
	 * Create a new result info object
	 * 
	 * @param sourcePage
	 *            The page that was used for copying
	 * @param createdPageCopy
	 *            The resulting copy
	 * @param targetFolder
	 *            The target folder for the copy
	 * @param targetChannelId
	 *            The target channel for the copy
	 */
	public PageCopyOpResultInfo(Page sourcePage, Page createdPageCopy, Folder targetFolder, Integer targetChannelId) {
		this.sourcePage = sourcePage;
		this.createdPageCopy = createdPageCopy;
		this.targetFolder = targetFolder;
		this.targetChannelId = targetChannelId;
	}

	/**
	 * Return the source page of this result info
	 * 
	 * @return
	 */
	public Page getSourcePage() {
		return sourcePage;
	}

	/**
	 * Return the created copy for this copy info
	 * 
	 * @return
	 */
	public Page getCreatedPageCopy() {
		return createdPageCopy;
	}

	/**
	 * Return the target folder for the copy info.
	 * 
	 * @return
	 */
	public Folder getTargetFolder() {
		return targetFolder;
	}

	/**
	 * Return the target channel of the created page for this copy info.
	 * 
	 * @return
	 */
	public Integer getTargetChannelId() {
		return targetChannelId;
	}

	@Override
	public String toString() {
		return "The new page copy " + createdPageCopy + " of source page " + sourcePage + " was copied to folder " + targetFolder + " in channel " + targetChannelId;
	}

}
