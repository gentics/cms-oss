/*
 * @author norbert
 * @date 28.04.2010
 * @version $Id: FileSaveRequest.java,v 1.1 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.File;

/**
 * Request to save a file
 * @author norbert
 */
@XmlRootElement
public class FileSaveRequest {

	/**
	 * File to be saved
	 */
	private File file;

	/**
	 * When true, saving the file with duplicate filename will fail, when false, the filename will be made unique before saving
	 */
	private Boolean failOnDuplicate;

	/**
	 * Constructor used by JAXB
	 */
	public FileSaveRequest() {}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * True if saving the file with a duplicate filename will fail. If false (default) the filename will be made unique before saving
	 * @return true or false
	 */
	public Boolean getFailOnDuplicate() {
		return failOnDuplicate;
	}

	/**
	 * Set whether saving shall fail on duplicate filenames
	 * @param failOnDuplicate true to fail on duplicate filenames
	 */
	public void setFailOnDuplicate(Boolean failOnDuplicate) {
		this.failOnDuplicate = failOnDuplicate;
	}
}
