/*
 * @author norbert
 * @date 07.03.2011
 * @version $Id: PagePreviewResponse.java,v 1.1.2.1 2011-03-07 16:37:17 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response for a page preview request
 */
@XmlRootElement
public class PagePreviewResponse extends GenericResponse {

	/**
	 * Page object that contains the loaded page
	 */
	private String preview;

	/**
	 * Empty constructor needed by JAXB
	 */
	public PagePreviewResponse() {}

	/**
	 * Creates a PageLoadResponse with the provided single message and ResponseInfo.
	 * 
	 * @param message The messages that should be displayed to the user
	 * @param response ResponseInfo with the status of the response
	 */
	public PagePreviewResponse(Message message, ResponseInfo responseInfo, String preview) {
		super(message, responseInfo);
		this.preview = preview;
	}

	public String getPreview() {
		return preview;
	}

	public void setPreview(String preview) {
		this.preview = preview;
	}
}
