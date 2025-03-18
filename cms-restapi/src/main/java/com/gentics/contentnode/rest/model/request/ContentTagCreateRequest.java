package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to create a content tag in a page
 */
@XmlRootElement
public class ContentTagCreateRequest extends TagCreateRequest {

	/**
	 * Id of the page from which the tag shall be copied
	 */
	private String copyPageId;

	/**
	 * Name of the tag from which the tag shall be copied
	 */
	private String copyTagname;

	/**
	 * Empty constructor
	 */
	public ContentTagCreateRequest() {}

	/**
	 * @return the copyPageId
	 */
	public String getCopyPageId() {
		return copyPageId;
	}

	/**
	 * @param copyPageId the copyPageId to set
	 */
	public void setCopyPageId(String copyPageId) {
		this.copyPageId = copyPageId;
	}

	/**
	 * @return the copyTagname
	 */
	public String getCopyTagname() {
		return copyTagname;
	}

	/**
	 * @param copyTagname the copyTagname to set
	 */
	public void setCopyTagname(String copyTagname) {
		this.copyTagname = copyTagname;
	}
}
