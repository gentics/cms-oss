package com.gentics.contentnode.rest.model.response;

import com.gentics.contentnode.rest.model.File;

/**
 * Response for file and image list requests.
 */
public class  FileListResponse extends AbstractListResponse<File> {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1180531953664474020L;

	public FileListResponse() {
	}

	public FileListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}
}
