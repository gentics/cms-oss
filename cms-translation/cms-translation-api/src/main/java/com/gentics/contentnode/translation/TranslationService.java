package com.gentics.contentnode.translation;

import java.util.function.Predicate;

/**
 * Interface for translation services
 */
public interface TranslationService {
	/**
	 * Translate a text to the specified language by specifying the source language.
	 *
	 * @param textToTranslate the text that should be translated
	 * @param sourceLanguage  the language of the source text.
	 * @param targetLanguage  the language that the text should be translated to.
	 * @return the translated text
	 * @throws Exception if an error occurs during translating
	 */
	String translate(String textToTranslate, String sourceLanguage, String targetLanguage)
			throws Exception;

}
