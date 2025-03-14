package com.gentics.contentnode.translation;

import com.gentics.contentnode.api.translation.TranslationService;

/**
 * Test translation service, that just returns the input as translation
 */
public class TestTranslationService implements TranslationService {
	@Override
	public String translate(String textToTranslate, String sourceLanguage, String targetLanguage) throws Exception {
		return textToTranslate;
	}
}
