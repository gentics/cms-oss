package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing the tag status for a template
 */
@XmlRootElement
public class TagStatusResponse extends AbstractListResponse<TagStatus> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3469185596107895096L;

	/**
	 * Create empty instance
	 */
	public TagStatusResponse() {
		super();
	}

	/**
	 * Create instance
	 * @param message message
	 * @param responseInfo response info
	 */
	public TagStatusResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}
}
