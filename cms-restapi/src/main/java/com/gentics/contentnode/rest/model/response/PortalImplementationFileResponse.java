package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.PortalImplementationFileModel;

/**
 * Response containing a file of a Portal Implementation
 */
@XmlRootElement
public class PortalImplementationFileResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1984303214651379506L;

	private PortalImplementationFileModel file;

	/**
	 * Create empty instance
	 */
	public PortalImplementationFileResponse() {
	}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public PortalImplementationFileResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with response info and file
	 * @param file file
	 * @param responseInfo response info
	 */
	public PortalImplementationFileResponse(PortalImplementationFileModel file, ResponseInfo responseInfo) {
		super(null, responseInfo);
		setFile(file);
	}

	/**
	 * File
	 * @return the file
	 */
	public PortalImplementationFileModel getFile() {
		return file;
	}

	/**
	 * Set the file
	 * @param file file
	 */
	public void setFile(PortalImplementationFileModel file) {
		this.file = file;
	}
}
