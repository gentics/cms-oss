package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing the current session language
 */
@XmlRootElement
public class LanguageResponse extends GenericResponse {

	/**
	 * Language code of the language
	 */
	protected String code;

	/**
	 * Create an empty instance
	 */
	public LanguageResponse() {}

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
