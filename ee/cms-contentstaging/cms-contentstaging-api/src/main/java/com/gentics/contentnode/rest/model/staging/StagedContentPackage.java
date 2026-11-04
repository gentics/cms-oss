package com.gentics.contentnode.rest.model.staging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.rest.resource.impl.internal.JobStatus;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Content staging package entity.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedContentPackage extends ContentPackageBase implements Resolvable {

	private static final long serialVersionUID = 8051924103845219672L;

	private Integer forms;
	private Integer files;
	private Integer pages;
	private Integer folders;
	private Integer images;
	private Integer nodes;
	private Integer channels;

	@XmlAttribute(name = "import")
	private JobStatus import_;

	public static final String TYPE_NAME = "contentpackage";

	/**
	 * Count of forms in the package.
	 * 
	 * @return forms count
	 */
	public Integer getForms() {
		return forms;
	}

	/**
	 * Set the count of forms in the package.
	 * @param forms count
	 */
	public void setForms(Integer forms) {
		this.forms = forms;
	}

	/**
	 * Count of files in the package.
	 * @return files count
	 */
	public Integer getFiles() {
		return files;
	}

	/**
	 * Set the count of files in the package.
	 * @param files count
	 */
	public void setFiles(Integer files) {
		this.files = files;
	}

	/**
	 * Count of pages in the package.
	 * @return pages count
	 */
	public Integer getPages() {
		return pages;
	}

	/**
	 * Set the count of pages in the package.
	 * @param pages count
	 */
	public void setPages(Integer pages) {
		this.pages = pages;
	}

	/**
	 * Count of folders in the package.
	 * @return folders count
	 */
	public Integer getFolders() {
		return folders;
	}

	/**
	 * Set the count of folders in the package.
	 * @param folders count
	 */
	public void setFolders(Integer folders) {
		this.folders = folders;
	}

	/**
	 * Count of images in the package.
	 * @return images count
	 */
	public Integer getImages() {
		return images;
	}

	/**
	 * Set the count of images in the package.
	 * @param images count
	 */
	public void setImages(Integer images) {
		this.images = images;
	}

	/**
	 * Count of nodes in the package.
	 * @return nodes count
	 */
	public Integer getNodes() {
		return nodes;
	}

	/**
	 * Set the count of nodes in the package.
	 * @param nodes count
	 */
	public void setNodes(Integer nodes) {
		this.nodes = nodes;
	}

	/**
	 * Count of channels in the package.
	 * @return channels count
	 */
	public Integer getChannels() {
		return channels;
	}

	/**
	 * Set the number of channels in the package.
	 * @param channels count
	 */
	public void setChannels(Integer channels) {
		this.channels = channels;
	}

	/**
	 * Status of the latest import
	 * 
	 * @return
	 */
	public JobStatus getImport() {
		return import_;
	}

	/**
	 * Set import job status.
	 * 
	 * @param import_
	 */
	public void setImport(JobStatus import_) {
		this.import_ = import_;
	}

	@Override
	public Object getProperty(String key) {
		return get(key);
	}

	@Override
	public Object get(String key) {
		switch (key) {
		case "name":
			return getName();
		case "description":
			return getDescription();
		case "timestamp":
			return getTimestamp();
		case "nodes":
			return getNodes();
		case "folders":
			return getFolders();
		case "pages":
			return getPages();
		case "files":
			return getFiles();
		case "images":
			return getImages();
		}
		return null;
	}

	@Override
	public boolean canResolve() {
		return true;
	}
}
