package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Response containing object counts in folders
 */
@XmlRootElement
public class FolderObjectCountResponse extends GenericResponse {

	/**
	 * Number of folders
	 */
	private int folders;

	/**
	 * Number of pages
	 */
	private int pages;

	/**
	 * Number of templates
	 */
	private int templates;

	/**
	 * Number of images
	 */
	private int images;

	/**
	 * Number of files
	 */
	private int files;

	/**
	 * Create an empty response object
	 */
	public FolderObjectCountResponse() {}

	/**
	 * Create a response object with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public FolderObjectCountResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * @return the folders
	 */
	public int getFolders() {
		return folders;
	}

	/**
	 * @return the pages
	 */
	public int getPages() {
		return pages;
	}

	/**
	 * @return the templates
	 */
	public int getTemplates() {
		return templates;
	}

	/**
	 * @return the images
	 */
	public int getImages() {
		return images;
	}

	/**
	 * @return the files
	 */
	public int getFiles() {
		return files;
	}

	/**
	 * @param folders the folders to set
	 */
	public void setFolders(int folders) {
		this.folders = folders;
	}

	/**
	 * @param pages the pages to set
	 */
	public void setPages(int pages) {
		this.pages = pages;
	}

	/**
	 * @param templates the templates to set
	 */
	public void setTemplates(int templates) {
		this.templates = templates;
	}

	/**
	 * @param images the images to set
	 */
	public void setImages(int images) {
		this.images = images;
	}

	/**
	 * @param files the files to set
	 */
	public void setFiles(int files) {
		this.files = files;
	}
}
