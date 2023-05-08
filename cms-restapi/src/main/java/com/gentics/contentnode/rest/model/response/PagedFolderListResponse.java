package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Folder;

/**
 * Response containing a list of folders
 */
@XmlRootElement
public class PagedFolderListResponse extends AbstractListResponse<Folder> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8370434869831287849L;

	/**
	 * Create empty instance
	 */
	public PagedFolderListResponse() {
		super();
	}

	/**
	 * Create instance
	 * @param message message
	 * @param responseInfo response info
	 */
	public PagedFolderListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

}
