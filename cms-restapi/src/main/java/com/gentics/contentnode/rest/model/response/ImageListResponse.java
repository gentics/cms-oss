package com.gentics.contentnode.rest.model.response;

import com.gentics.contentnode.rest.model.Image;

/**
 * Response for file and image list requests.
 */
public class ImageListResponse extends AbstractStagingStatusListResponse<Image> {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2414917611951124114L;

	public ImageListResponse() {
	}

	public ImageListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}
}
