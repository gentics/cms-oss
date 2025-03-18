package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Base class for translation requests and responses.
 */
@XmlRootElement
public class TranslationDto implements Serializable {

	private static final long serialVersionUID = -7483178254386691173L;

	/**
	 * The text to be translated for requests / The translated text for responses.
	 */
	private String text;

	/**
	 * This method is used to get the translation text.
	 * @return String This returns the translation text.
	 */
	public String getText() {
		return text;
	}

	/**
	 * This method is used to set the translation text.
	 * @param text The translation text
	 */
	public void setText(String text) {
		this.text = text;
	}

}
