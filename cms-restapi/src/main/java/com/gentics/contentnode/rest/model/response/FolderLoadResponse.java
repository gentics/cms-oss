/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: FolderLoadResponse.java,v 1.2 2010-09-16 15:09:58 clemens Exp $
 */
package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Folder;

/**
 * @author norbert
 *
 */
@XmlRootElement
public class FolderLoadResponse extends GenericResponse {

	private static final long serialVersionUID = -4157005507718644867L;

	/**
	 * Folder returned by this response
	 */
	private Folder folder;

	/**
	 * Staging package inclusion status
	 */
	private StagingStatus stagingStatus;

	/**
	 * Constructor used by JAXB
	 */
	public FolderLoadResponse() {}

	/**
	 * Create an instance of the response with single message, response info and folder
	 * @param message message
	 * @param responseInfo response info
	 * @param folder folder
	 */
	public FolderLoadResponse(Message message, ResponseInfo responseInfo, Folder folder) {
		super(message, responseInfo);
		this.folder = folder;
	}

	/**
	 * @return the folder
	 */
	public Folder getFolder() {
		return folder;
	}

	/**
	 * @param folder the folder to set
	 */
	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public StagingStatus getStagingStatus() {
		return stagingStatus;
	}

	public void setStagingStatus(StagingStatus stagingStatus) {
		this.stagingStatus = stagingStatus;
	}
}
