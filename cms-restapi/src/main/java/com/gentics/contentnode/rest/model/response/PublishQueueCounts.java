package com.gentics.contentnode.rest.model.response;

import java.io.Serializable;
import java.util.Objects;

import com.gentics.contentnode.rest.model.response.admin.ObjectCount;

/**
 * REST Model for object counts
 */
public class PublishQueueCounts implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1557405864441979188L;

	protected ObjectCount files;

	protected ObjectCount folders;

	protected ObjectCount forms;

	protected ObjectCount pages;

	/**
	 * File counts
	 * @return file counts object
	 */
	public ObjectCount getFiles() {
		return files;
	}

	/**
	 * Set the file counts
	 * @param files file counts
	 * @return fluent API
	 */
	public PublishQueueCounts setFiles(ObjectCount files) {
		this.files = files;
		return this;
	}

	/**
	 * Folder counts
	 * @return folder counts object
	 */
	public ObjectCount getFolders() {
		return folders;
	}

	/**
	 * Set the folder counts
	 * @param folders folder counts
	 * @return fluent API
	 */
	public PublishQueueCounts setFolders(ObjectCount folders) {
		this.folders = folders;
		return this;
	}

	/**
	 * Forms counts
	 * @return form counts object
	 */
	public ObjectCount getForms() {
		return forms;
	}

	/**
	 * Set the form counts
	 * @param forms form counts
	 * @return fluent API
	 */
	public PublishQueueCounts setForms(ObjectCount forms) {
		this.forms = forms;
		return this;
	}

	/**
	 * Page counts
	 * @return page counts object
	 */
	public ObjectCount getPages() {
		return pages;
	}

	/**
	 * Set the page counts
	 * @param pages page counts
	 * @return fluent API
	 */
	public PublishQueueCounts setPages(ObjectCount pages) {
		this.pages = pages;
		return this;
	}

	@Override
	public String toString() {
		return String.format("files: [%s], folders: [%s], pages: [%s]", files, folders, pages);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PublishQueueCounts) {
			PublishQueueCounts other = (PublishQueueCounts) obj;
			return Objects.equals(files, other.files) && Objects.equals(folders, other.folders)
					&& Objects.equals(pages, other.pages);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(files, folders, pages);
	}
}
