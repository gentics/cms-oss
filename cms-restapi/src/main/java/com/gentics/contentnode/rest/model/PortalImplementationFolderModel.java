package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model of a folder in a Portal Implementation. Folders are not stored separately, they exist as long as they contain at least one file.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class PortalImplementationFolderModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6203916178434187752L;

	/**
	 * Path of the folder
	 */
	private String path;

	/**
	 * Name of the folder
	 */
	private String name;

	/**
	 * Names of the direct subfolders
	 */
	private List<String> folders;

	/**
	 * Files directly contained in the folder
	 */
	private List<PortalImplementationFileModel> files;

	/**
	 * Path of the folder, relative to the root folder of the Portal Implementation (empty for the root folder). Folders are separated by <code>/</code>.
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the path
	 * @param path path
	 * @return fluent API
	 */
	public PortalImplementationFolderModel setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * Name of the folder (last segment of the path, empty for the root folder)
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 * @return fluent API
	 */
	public PortalImplementationFolderModel setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Names of the direct subfolders, sorted by name
	 * @return list of subfolder names
	 */
	public List<String> getFolders() {
		return folders;
	}

	/**
	 * Set the subfolder names
	 * @param folders subfolder names
	 * @return fluent API
	 */
	public PortalImplementationFolderModel setFolders(List<String> folders) {
		this.folders = folders;
		return this;
	}

	/**
	 * Files directly contained in the folder, sorted by name
	 * @return list of files
	 */
	public List<PortalImplementationFileModel> getFiles() {
		return files;
	}

	/**
	 * Set the files
	 * @param files files
	 * @return fluent API
	 */
	public PortalImplementationFolderModel setFiles(List<PortalImplementationFileModel> files) {
		this.files = files;
		return this;
	}
}
