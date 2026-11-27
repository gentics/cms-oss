package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model of a file contained in a Portal Implementation
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class PortalImplementationFileModel extends AbstractModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4123866402573164312L;

	/**
	 * Local ID
	 */
	private Integer id;

	/**
	 * Path including the filename
	 */
	private String path;

	/**
	 * Filename
	 */
	private String name;

	/**
	 * Path of the folder
	 */
	private String folder;

	/**
	 * Mimetype
	 */
	private String mimetype;

	/**
	 * Filesize in bytes
	 */
	private long filesize;

	/**
	 * Creator
	 */
	private User creator;

	/**
	 * Creation timestamp
	 */
	private int cdate;

	/**
	 * Last editor
	 */
	private User editor;

	/**
	 * Timestamp of last editing
	 */
	private int edate;

	/**
	 * Local ID
	 * @return local ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the local ID
	 * @param id ID
	 * @return fluent API
	 */
	public PortalImplementationFileModel setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Path of the file (including the filename), relative to the root folder of the Portal Implementation. Folders are separated by <code>/</code>.
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
	public PortalImplementationFileModel setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * Filename (last segment of the path)
	 * @return filename
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the filename
	 * @param name filename
	 * @return fluent API
	 */
	public PortalImplementationFileModel setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Path of the folder containing the file (empty for files in the root folder)
	 * @return folder path
	 */
	public String getFolder() {
		return folder;
	}

	/**
	 * Set the folder path
	 * @param folder folder path
	 * @return fluent API
	 */
	public PortalImplementationFileModel setFolder(String folder) {
		this.folder = folder;
		return this;
	}

	/**
	 * Mimetype
	 * @return mimetype
	 */
	public String getMimetype() {
		return mimetype;
	}

	/**
	 * Set the mimetype
	 * @param mimetype mimetype
	 * @return fluent API
	 */
	public PortalImplementationFileModel setMimetype(String mimetype) {
		this.mimetype = mimetype;
		return this;
	}

	/**
	 * Filesize in bytes
	 * @return filesize
	 */
	public long getFilesize() {
		return filesize;
	}

	/**
	 * Set the filesize
	 * @param filesize filesize
	 * @return fluent API
	 */
	public PortalImplementationFileModel setFilesize(long filesize) {
		this.filesize = filesize;
		return this;
	}

	/**
	 * Creator
	 * @return creator
	 */
	public User getCreator() {
		return creator;
	}

	/**
	 * Set the creator
	 * @param creator creator
	 * @return fluent API
	 */
	public PortalImplementationFileModel setCreator(User creator) {
		this.creator = creator;
		return this;
	}

	/**
	 * Creation timestamp
	 * @return creation timestamp
	 */
	public int getCdate() {
		return cdate;
	}

	/**
	 * Set the creation timestamp
	 * @param cdate creation timestamp
	 * @return fluent API
	 */
	public PortalImplementationFileModel setCdate(int cdate) {
		this.cdate = cdate;
		return this;
	}

	/**
	 * Last editor
	 * @return last editor
	 */
	public User getEditor() {
		return editor;
	}

	/**
	 * Set the last editor
	 * @param editor last editor
	 * @return fluent API
	 */
	public PortalImplementationFileModel setEditor(User editor) {
		this.editor = editor;
		return this;
	}

	/**
	 * Timestamp of last editing
	 * @return edit timestamp
	 */
	public int getEdate() {
		return edate;
	}

	/**
	 * Set the timestamp of last editing
	 * @param edate edit timestamp
	 * @return fluent API
	 */
	public PortalImplementationFileModel setEdate(int edate) {
		this.edate = edate;
		return this;
	}
}
