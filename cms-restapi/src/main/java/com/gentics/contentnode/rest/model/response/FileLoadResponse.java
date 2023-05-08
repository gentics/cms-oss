/*
 * @author norbert
 * @date 28.04.2010
 * @version $Id: FileLoadResponse.java,v 1.1 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.File;

/**
 * Response object for responses containing a File
 * @author norbert
 */
@XmlRootElement
public class FileLoadResponse extends GenericResponse {

	private static final long serialVersionUID = -4471168741484931008L;
	/**
	 * File contained in the response
	 */
	private File file;

	/**
	 * Staging package inclusion status
	 */
	private StagingStatus stagingStatus;

	/**
	 * Constructor used by JAXB
	 */
	public FileLoadResponse() {}

	/**
	 * Constructor with message, response info and file
	 * @param message message
	 * @param responseInfo response info
	 * @param file
	 */
	public FileLoadResponse(Message message, ResponseInfo responseInfo, File file) {
		super(message, responseInfo);
		this.file = file;
	}

	/**
	 * @return the file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file the file to set
	 */
	public void setFile(File file) {
		this.file = file;
	}

	public StagingStatus getStagingStatus() {
		return stagingStatus;
	}

	public void setStagingStatus(StagingStatus stagingStatus) {
		this.stagingStatus = stagingStatus;
	}
}
