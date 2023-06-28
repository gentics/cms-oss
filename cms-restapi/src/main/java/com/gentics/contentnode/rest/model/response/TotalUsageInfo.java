package com.gentics.contentnode.rest.model.response;

import java.util.Objects;

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
	 * @return fluent API
	 */
	public TotalUsageInfo setTotal(int total) {
		this.total = total;
		return this;
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
	 * @return fluent API
	 */
	public TotalUsageInfo setPages(Integer pages) {
		this.pages = pages;
		return this;
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
	 * @return fluent API
	 */
	public TotalUsageInfo setFolders(Integer folders) {
		this.folders = folders;
		return this;
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
	 * @return fluent API
	 */
	public TotalUsageInfo setTemplates(Integer templates) {
		this.templates = templates;
		return this;
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
	 * @return fluent API
	 */
	public TotalUsageInfo setFiles(Integer files) {
		this.files = files;
		return this;
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
	 * @return fluent API
	 */
	public TotalUsageInfo setImages(Integer images) {
		this.images = images;
		return this;
	}

	@Override
	public String toString() {
		StringBuffer info = new StringBuffer();
		info.append(String.format("total: %d", total));
		if (pages != null) {
			info.append(String.format(", pages: %d", pages));
		}
		if (templates != null) {
			info.append(String.format(", templates: %d", templates));
		}
		if (folders != null) {
			info.append(String.format(", folders: %d", folders));
		}
		if (images != null) {
			info.append(String.format(", images: %d", images));
		}
		if (files != null) {
			info.append(String.format(", files: %d", files));
		}
		return info.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(total, pages, templates, folders, images, files);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TotalUsageInfo) {
			TotalUsageInfo other = (TotalUsageInfo) obj;
			return Objects.equals(total, other.total) && Objects.equals(pages, other.pages)
					&& Objects.equals(templates, other.templates) && Objects.equals(folders, other.folders)
					&& Objects.equals(images, other.images) && Objects.equals(files, other.files);
		} else {
			return false;
		}
	}
}
