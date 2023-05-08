/*
 * @author norbert
 * @date 28.04.2010
 * @version $Id: FileSaveRequest.java,v 1.1 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.request.page.TargetFolder;

/**
 * Request to copy a file
 * @author johannes2
 */
@XmlRootElement
public class FileCopyRequest {

	/**
	 * File to be copied
	 */
	private File file;

	private String newFilename;

	private Integer nodeId;

	private TargetFolder targetFolder;

	/**
	 * Constructor used by JAXB
	 */
	public FileCopyRequest() {}

	/**
	 * Get the new filename for the copied file being created.
	 * @return
	 */
	public String getNewFilename() {
		return newFilename;
	}

	/**
	 * Set the new filename for the copied file being created.
	 * @param newFilename
	 */
	public void setNewFilename(String newFilename) {
		this.newFilename = newFilename;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * Get the node ID the file should be copied to.
	 * @return node ID
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the target folder the file should be copied to.
	 * @return target folder
	 */
	public TargetFolder getTargetFolder() {
		return targetFolder;
	}

	public void setTargetFolder(TargetFolder targetFolder) {
		this.targetFolder = targetFolder;
	}
}
