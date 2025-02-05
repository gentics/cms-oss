package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to set the session language
 */
@XmlRootElement
public class SetLanguageRequest {

	/**
	 * Language code of the language to be set
	 */
	protected String code;

	/**
	 * Create an empty instance
	 */
	public SetLanguageRequest() {}

	/**
	 * Get the language code
	 * @return language code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Set the language code
	 * @param code language code
	 */
	public void setCode(String code) {
		this.code = code;
	}
}
