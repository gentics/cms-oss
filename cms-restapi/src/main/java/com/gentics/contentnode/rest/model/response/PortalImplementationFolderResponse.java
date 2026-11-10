package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.PortalImplementationFolderModel;

/**
 * Response containing a folder of a Portal Implementation
 */
@XmlRootElement
public class PortalImplementationFolderResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5543160279112868043L;

	private PortalImplementationFolderModel folder;

	/**
	 * Create empty instance
	 */
	public PortalImplementationFolderResponse() {
	}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public PortalImplementationFolderResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with response info and folder
	 * @param folder folder
	 * @param responseInfo response info
	 */
	public PortalImplementationFolderResponse(PortalImplementationFolderModel folder, ResponseInfo responseInfo) {
		super(null, responseInfo);
		setFolder(folder);
	}

	/**
	 * Folder
	 * @return the folder
	 */
	public PortalImplementationFolderModel getFolder() {
		return folder;
	}

	/**
	 * Set the folder
	 * @param folder folder
	 */
	public void setFolder(PortalImplementationFolderModel folder) {
		this.folder = folder;
	}
}
