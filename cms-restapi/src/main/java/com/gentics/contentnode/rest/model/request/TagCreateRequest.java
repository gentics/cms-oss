/*
 * @author norbert
 * @date 29.11.2010
 * @version $Id: TagCreateRequest.java,v 1.1.2.1 2010-11-29 12:34:40 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Tag create request
 * @author norbert
 */
@XmlRootElement
public class TagCreateRequest {

	/**
	 * Magic value
	 */
	private String magicValue;

	/**
	 * Construct ID
	 */
	private Integer constructId;

	/**
	 * Keyword
	 */
	private String keyword;

	/**
	 * Create an empty instance
	 */
	public TagCreateRequest() {}

	/**
	 * Set the magic value
	 * @param magicValue magic value
	 */
	public void setMagicValue(String magicValue) {
		this.magicValue = magicValue;
	}

	/**
	 * Get the magic value
	 * @return magic value
	 */
	public String getMagicValue() {
		return magicValue;
	}

	/**
	 * @return the constructId
	 */
	public Integer getConstructId() {
		return constructId;
	}

	/**
	 * @param constructId the constructId to set
	 */
	public void setConstructId(Integer constructId) {
		this.constructId = constructId;
	}

	/**
	 * @return the keyword
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * @param keyword the keyword to set
	 */
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
}
