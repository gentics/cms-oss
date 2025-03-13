package com.gentics.contentnode.rest.model.request;

import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request containing the objects selected for export
 */
@XmlRootElement
public class ExportSelectionRequest {

	/**
	 * List of selected pages
	 */
	protected List<Integer> pages;

	/**
	 * List of selected images
	 */
	protected List<Integer> images;

	/**
	 * List of selected files
	 */
	protected List<Integer> files;

	/**
	 * List of selected folders
	 */
	protected List<Integer> folders;

	/**
	 * Map of selected inherited folders for channels
	 */
	protected Map<Integer, List<Integer>> inheritedFolders;

	/**
	 * Empty constructor
	 */
	public ExportSelectionRequest() {}

	/**
	 * Get selected pages
	 * @return the pages
	 */
	public List<Integer> getPages() {
		return pages;
	}

	/**
	 * Set selected pages
	 * @param pages the pages to set
	 */
	public void setPages(List<Integer> pages) {
		this.pages = pages;
	}

	/**
	 * Get selected images
	 * @return the images
	 */
	public List<Integer> getImages() {
		return images;
	}

	/**
	 * Set selected images
	 * @param images the images to set
	 */
	public void setImages(List<Integer> images) {
		this.images = images;
	}

	/**
	 * Get selected files
	 * @return the files
	 */
	public List<Integer> getFiles() {
		return files;
	}

	/**
	 * Set selected files
	 * @param files the files to set
	 */
	public void setFiles(List<Integer> files) {
		this.files = files;
	}

	/**
	 * Get selected folders
	 * @return the folders
	 */
	public List<Integer> getFolders() {
		return folders;
	}

	/**
	 * Set selected folders
	 * @param folders the folders to set
	 */
	public void setFolders(List<Integer> folders) {
		this.folders = folders;
	}

	/**
	 * Get the inherited folders
	 * @return the inheritedFolders
	 */
	public Map<Integer, List<Integer>> getInheritedFolders() {
		return inheritedFolders;
	}

	/**
	 * Set the inherited folders
	 * @param inheritedFolders the inheritedFolders to set
	 */
	public void setInheritedFolders(Map<Integer, List<Integer>> inheritedFolders) {
		this.inheritedFolders = inheritedFolders;
	}
}
