package com.gentics.contentnode.translation;

import com.gentics.api.lib.exception.NodeException;
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
	 * @throws NodeException if an error occurs during translating
	 */
	String translate(String textToTranslate, String sourceLanguage, String targetLanguage)
			throws NodeException;


	/**
	 * @param htmlToTranslate the text containing markup to translate
	 * @param sourceLanguage  the language of the source text.
	 * @param targetLanguage  the language that the text should be translated to.
	 * @param validator       a validator function that checks if the markup is valid
	 * @return The translated text
	 * @throws NodeException if the translated markup fails the validation or an error occur during translating.
	 */
	default String translate(String htmlToTranslate, String sourceLanguage, String targetLanguage,
			Predicate<String> validator) throws NodeException {
		var translatedMarkup = this.translate(htmlToTranslate, sourceLanguage, targetLanguage);

		if (!validator.test(translatedMarkup)) {
			throw new NodeException("Translated markup is invalid");
		}

		return translatedMarkup;
	}


	/**
	 * Translate a text to the specified language. The source language will be autodetected.
	 *
	 * @param textToTranslate the text that should be translated
	 * @param targetLanguage  the language that the text should be translated to.
	 * @return the translated text
	 * @throws NodeException if an error occurs during translating
	 */
	String translate(String textToTranslate, String targetLanguage) throws NodeException;
}
