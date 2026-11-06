package com.gentics.contentnode.rest.model.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ContentNodeItem.ItemType;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Page;

/**
 * Response containing directly linked objects and indirect dependencies for
 * an object that should be synchronised to a master channel.
 */
@XmlRootElement
public class ReferencedSyncableObjectsListResponse extends GenericResponse {
	/**
	 * Type alias for <code>Map&lt;Integer, Map&lt;ItemType, Map&lt;Integer, Integer&gt;&gt;&gt;<code> to reduce clutter.
	 */
	public static class SyncDependencies extends HashMap<Integer, Map<ItemType, Map<Integer, Integer>>> {
		private static final long serialVersionUID = -8138202673876599475L;
	}

	private static final long serialVersionUID = -2014561397142326146L;

	/** A map from item type to dependencies of that type. */
	private Map<ItemType, SyncDependencies> dependencies = new HashMap<>();

	/** Directly linked pages. */
	private List<Page> pages;
	/** The number of directly linked pages. */
	private int pagesTotal;
	/** The number of pages not visible to the current user. */
	private int pagesWithoutPermission;
	/** Directly linked files. */
	private List<File> files;
	/** The number of directly linked files. */
	private int filesTotal;
	/** The number of files not visible to the current user. */
	private int filesWithoutPermission;
	/** Directly linked images. */
	private List<Image> images;
	/** The number of directly linked images. */
	private int imagesTotal;
	/** The number of images not visible to the current user. */
	private int imagesWithoutPermission;

	/**
	 * Constructor from {@link GenericResponse}.
	 *
	 * @param message The message that should be displayed to the user
	 * @param responseInfo ResponseInfo with the status of the response
	 */
	public ReferencedSyncableObjectsListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Default constructor with no message and a response info containing just the
	 * response code <code>OK</code>.
	 *
	 * @param dependencies The map of indirect dependencies
	 * @param pages The directly linked pages
	 * @param pagesWithoutPermission The number of pages not visible to the current user
	 * @param files The directly linked files
	 * @param filesWithoutPermission The number of files not visible to the current user
	 * @param images The directly linked images
	 * @param imagesWithoutPermission The number of images not visible to the current user
	 */
	public ReferencedSyncableObjectsListResponse(
			Map<ItemType, SyncDependencies> dependencies,
			List<Page> pages,
			int pagesWithoutPermission,
			List<File> files,
			int filesWithoutPermission,
			List<Image> images,
			int imagesWithoutPermission) {
		super(null, new ResponseInfo(ResponseCode.OK, ""));

		this.dependencies = dependencies;
		this.pages = pages;
		this.pagesTotal = pages.size();
		this.pagesWithoutPermission = pagesWithoutPermission;
		this.files = files;
		this.filesTotal = files.size();
		this.filesWithoutPermission = filesWithoutPermission;
		this.images = images;
		this.imagesTotal = images.size();
		this.imagesWithoutPermission = imagesWithoutPermission;
	}

	/**
	 * Flag indicating if there are any dependencies at all.
	 *
	 * @return <code>true</code> if there are any direct or indirect dependencies, and
	 * 	<code>false</code> otherwise.
	 */
	public boolean isHaveDependencies() {
		return pagesTotal + filesTotal + imagesTotal + dependencies.size() > 0;
	}

	/**
	 * Get the indirect dependencies.
	 *
	 * @return The indirect dependencies
	 */
	public Map<ItemType, SyncDependencies> getDependencies() {
		return dependencies;
	}

	/**
	 * Set the indirect dependencies.
	 *
	 * @param dependencies The indirect dependencies
	 */
	public void setDependencies(Map<ItemType, SyncDependencies> dependencies) {
		this.dependencies = dependencies;
	}

	/**
	 * Get the directly linked pages.
	 *
	 * @return The directly linked pages
	 */
	public List<Page> getPages() {
		return pages;
	}

	/**
	 * Set the directly linked pages.
	 *
	 * @param pages The directly linked pages
	 */
	public void setPages(List<Page> pages) {
		this.pages = pages;
	}

	/**
	 * Get the number of directly linked pages.
	 *
	 * @return The number of directly linked pages.
	 */
	public int getPagesTotal() {
		return pagesTotal;
	}

	/**
	 * Set the number of directly linked pages.
	 *
	 * @param pagesTotal The number of directly linked pages
	 */
	public void setPagesTotal(int pagesTotal) {
		this.pagesTotal = pagesTotal;
	}

	/**
	 * Get the number of pages not visible to the current user.
	 *
	 * @return The number of pages not visible to the current user
	 */
	public int getPagesWithoutPermission() {
		return pagesWithoutPermission;
	}

	/**
	 * Set the number of pages not visible to the current user.
	 * @param pagesWithoutPermission The number of pages not visible to the current user
	 */
	public void setPagesWithoutPermission(int pagesWithoutPermission) {
		this.pagesWithoutPermission = pagesWithoutPermission;
	}

	/**
	 * Get the directly linked files.
	 *
	 * @return The directly linked files
	 */
	public List<File> getFiles() {
		return files;
	}

	/**
	 * Set the directly linked files.
	 *
	 * @param files The directly linked files
	 */
	public void setFiles(List<File> files) {
		this.files = files;
	}

	/**
	 * Get the number of directly linked files.
	 *
	 * @return The number of directly linked files
	 */
	public int getFilesTotal() {
		return filesTotal;
	}

	/**
	 * Set the number of directly linked files.
	 *
	 * @param filesTotal The number of directly linked files
	 */
	public void setFilesTotal(int filesTotal) {
		this.filesTotal = filesTotal;
	}

	/**
	 * Get the number of files not visible to the current user.
	 *
	 * @return The number of files not visible to the current user
	 */
	public int getFilesWithoutPermission() {
		return filesWithoutPermission;
	}

	/**
	 * Set the number of files not visible to the current user.
	 *
	 * @param filesWithoutPermission The number of files not visible to the current user
	 */
	public void setFilesWithoutPermission(int filesWithoutPermission) {
		this.filesWithoutPermission = filesWithoutPermission;
	}

	/**
	 * Get the directly linked images.
	 *
	 * @return The directly linked images
	 */
	public List<Image> getImages() {
		return images;
	}

	/**
	 * Set the directly linked images
	 *
	 * @param images The directly linked images
	 */
	public void setImages(List<Image> images) {
		this.images = images;
	}

	/**
	 * Get the number of directly linked images.
	 *
	 * @return The number of directly linked images
	 */
	public int getImagesTotal() {
		return imagesTotal;
	}

	/**
	 * Set the number of directly linked images
	 *
	 * @param imagesTotal The number of directly linked images
	 */
	public void setImagesTotal(int imagesTotal) {
		this.imagesTotal = imagesTotal;
	}

	/**
	 * Get the number of images not visible to the current user.
	 *
	 * @return The number of images not visible to the current user
	 */
	public int getImagesWithoutPermission() {
		return imagesWithoutPermission;
	}

	/**
	 * Set the number of images not visible to the current user
	 *
	 * @param imagesWithoutPermission The number of images not visible to the current user
	 */
	public void setImagesWithoutPermission(int imagesWithoutPermission) {
		this.imagesWithoutPermission = imagesWithoutPermission;
	}
}
