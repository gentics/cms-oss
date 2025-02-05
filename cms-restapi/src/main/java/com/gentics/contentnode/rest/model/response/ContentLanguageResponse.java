package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ContentLanguage;

/**
 * Response containing a single content language
 */
@XmlRootElement
public class ContentLanguageResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2023344286436348788L;

	private ContentLanguage language;

	/**
	 * Create empty instance
	 */
	public ContentLanguageResponse() {
	}

	/**
	 * Create instance with language and response info
	 * @param info response info
	 * @param language language
	 */
	public ContentLanguageResponse(ResponseInfo info, ContentLanguage language) {
		super(null, info);
		setLangage(language);
	}

	/**
	 * Content Language
	 * @return language
	 */
	public ContentLanguage getLanguage() {
		return language;
	}

	/**
	 * Set language
	 * @param language language
	 */
	public void setLangage(ContentLanguage language) {
		this.language = language;
	}
}
