package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TotalUsageInfo {

	/**
	 * Total number of elements using the queried element/s.
	 */
	private int total = 0;

	/**
	 * Number of pages which use the queried element/s.
	 */
	private Integer pages = null;

	/**
	 * Number of templates which use the queried element/s.
	 */
	private Integer templates = null;

	/**
	 * Number of folders which use the queried element/s.
	 */
	private Integer folders = null;

	/**
	 * Number of images which use the queried element/s.
	 */
	private Integer images = null;

	/**
	 * Number of files which use the queried element/s.
	 */
	private Integer files = null;

	/**
	 * Get the total number of all elements which reference one or more
	 * specified source elements.
	 * 
	 * @return total number of elements
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * Set the total number of all elements which use one or more source
	 * elements.
	 * 
	 * @param total
	 *            total number of elements
	 */
	public void setTotal(int total) {
		this.total = total;
	}

	/**
	 * Get the total number of pages.
	 * 
	 * @return
	 */
	public Integer getPages() {
		return pages;
	}

	/**
	 * Set the total number of pages.
	 * 
	 * @param pages
	 */
	public void setPages(Integer pages) {
		this.pages = pages;
	}

	/**
	 * Get the total number of folder.
	 * 
	 * @return
	 */
	public Integer getFolders() {
		return folders;
	}

	/**
	 * Set the total number of folder.
	 * 
	 * @param folders
	 */
	public void setFolders(Integer folders) {
		this.folders = folders;
	}

	/**
	 * Get the total number of template.
	 * 
	 * @return
	 */
	public Integer getTemplates() {
		return templates;
	}

	/**
	 * Set the total number of templates.
	 * 
	 * @param templates
	 */
	public void setTemplates(Integer templates) {
		this.templates = templates;
	}

	/**
	 * Get the total number of files.
	 * 
	 * @return
	 */
	public Integer getFiles() {
		return files;
	}

	/**
	 * Set the total number of files.
	 * 
	 * @param files
	 */
	public void setFiles(Integer files) {
		this.files = files;
	}

	/**
	 * Get the total number of images.
	 * 
	 * @return
	 */
	public Integer getImages() {
		return images;
	}

	/**
	 * Set the total number of images.
	 * 
	 * @param images
	 */
	public void setImages(Integer images) {
		this.images = images;
	}

}
