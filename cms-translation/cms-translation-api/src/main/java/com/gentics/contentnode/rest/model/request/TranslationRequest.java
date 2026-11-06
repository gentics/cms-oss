package com.gentics.contentnode.rest.model.request;

import com.gentics.contentnode.rest.model.TranslationDto;

/**
 * Request to translate a text into the desired language
 */
public class TranslationRequest extends TranslationDto {

	private static final long serialVersionUID = 8481093705987769677L;

	/**
	 * The language the text should be translated to
	 */
	public String targetLanguage;

	/**
	 * The language of the source text
	 */
	public String sourceLanguage;


	/**
	 * This method is used to get the source language.
	 * @return String This returns the current source language.
	 */
	public String getSourceLanguage() {
		return sourceLanguage;
	}

	/**
	 * This method is used to set the source language.
	 * @param sourceLanguage This is the source language to be set.
	 */
	public void setSourceLanguage(String sourceLanguage) {
		this.sourceLanguage = sourceLanguage;
	}


	/**
	 * This method is used to get the target language.
	 * @return String This returns the current target language.
	 */
	public String getTargetLanguage() {
		return targetLanguage;
	}

	/**
	 * This method is used to set the target language.
	 * @param targetLanguage This is the target language to be set.
	 */
	public void setTargetLanguage(String targetLanguage) {
		this.targetLanguage = targetLanguage;
	}

}
