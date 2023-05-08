/*
 * @author norbert
 * @date 28.06.2010
 * @version $Id: TagCreateResponse.java,v 1.1 2010-06-29 08:38:11 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Tag;

/**
 * Resonse for a tag create request
 */
@XmlRootElement
public class TagCreateResponse extends GenericResponse {

	/**
	 * Tag object containing the new created tag
	 */
	private Tag tag;

	/**
	 * Empty constructor
	 */
	public TagCreateResponse() {}

	/**
	 * @param message
	 * @param responseInfo
	 */
	public TagCreateResponse(Message message, ResponseInfo responseInfo, Tag tag) {
		super(message, responseInfo);
		this.tag = tag;
	}

	/**
	 * Get the tag
	 * @return the tag
	 */
	public Tag getTag() {
		return tag;
	}

	/**
	 * Set the tag
	 * @param tag the tag to set
	 */
	public void setTag(Tag tag) {
		this.tag = tag;
	}
}
