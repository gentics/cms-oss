package com.gentics.contentnode.rest.model.response;

import com.gentics.contentnode.rest.model.Page;

/**
 * Response for page list requests.
 */
public class PageListResponse extends AbstractStagingStatusListResponse<Page> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2892369677519795549L;

	public PageListResponse() {
	}

	public PageListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}
}
