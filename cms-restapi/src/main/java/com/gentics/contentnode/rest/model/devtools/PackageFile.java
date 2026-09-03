package com.gentics.contentnode.rest.model.devtools;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * A file or directory contained in the "files-internal" storage of a devtool package
 */
@XmlRootElement
public class PackageFile implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5192449542112750638L;

	private String path;

	private String name;

	private boolean directory;

	private long size;

	private int lastModified;

	/**
	 * Path of the file or directory, relative to the package's files-internal storage root
	 * @return relative path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the relative path
	 * @param path relative path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Name of the file or directory (last path segment)
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * True if this entry is a directory, false if it is a file
	 * @return directory flag
	 */
	public boolean isDirectory() {
		return directory;
	}

	/**
	 * Set the directory flag
	 * @param directory directory flag
	 */
	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	/**
	 * Size of the file in bytes (0 for directories)
	 * @return size in bytes
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Set the size
	 * @param size size in bytes
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * Timestamp of the last modification (seconds since epoch)
	 * @return last modified timestamp
	 */
	public int getLastModified() {
		return lastModified;
	}

	/**
	 * Set the last modified timestamp
	 * @param lastModified last modified timestamp
	 */
	public void setLastModified(int lastModified) {
		this.lastModified = lastModified;
	}
}
