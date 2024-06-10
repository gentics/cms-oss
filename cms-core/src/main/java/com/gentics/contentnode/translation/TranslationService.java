package com.gentics.contentnode.translation;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.StageableNodeObject;
import com.gentics.contentnode.rest.model.response.StagingStatus;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface for the translation service.
 */
public interface TranslationService {
	/**
	 * Translate a text to the specified language by specifying the source language.
	 * 
	 * @param textToTranslate The text that should be translated
	 * @param sourceLanguage The language of the source text.
	 * @param targetLanguage The language that the text should be translated to.
	 * @return The translated text
	 * @throws NodeException
	 */
	String translate(String textToTranslate, String sourceLanguage, String targetLanguage) throws NodeException;


	/**
	 * Translate a text to the specified language. The source language will be autodetected.
	 *
	 * @param textToTranslate The text that should be translated
	 * @param targetLanguage The language that the text should be translated to.
	 * @return The translated text
	 * @throws NodeException
	 */
	String translate(String textToTranslate, String targetLanguage) throws NodeException;
}
