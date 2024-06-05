package com.gentics.contentnode.translation;

import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.log.NodeLogger;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;

public class DeepLTranslationService implements TranslationService {

	static final SupportedProvider PROVIDER = SupportedProvider.DEEPL;
	final Translator translator;
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());
	Map<String, Object> languagePreferences;


	public DeepLTranslationService() throws NodeException {
		final var authToken = getAuthTokenFromConfig();
		languagePreferences = getLanguageSettingFromConfig();
		translator = new Translator(authToken);
	}

	private String getAuthTokenFromConfig() throws NodeException {
		var authToken = ConfigurationValue.TRANSLATION_AUTH_TOKEN.get();

		if (StringUtils.isBlank(authToken)) {
			throw new NodeException("Invalid authentication provided for: " + PROVIDER);
		}

		return authToken;
	}

	private Map<String, Object> getLanguageSettingFromConfig() {
		var languagePreferences = ConfigurationValue.TRANSLATION_LANGUAGE_PREFERENCES.getMap();

		if (MapUtils.isEmpty(languagePreferences)) {
			final var errorMessage = String.format(
					"No language preferences found for: '%s'. Using default configuration.", PROVIDER);
			logger.log(Level.INFO, errorMessage);
		}

		return languagePreferences;
	}

	@Override
	public String translate(String textToTranslate, String targetLanguage) throws NodeException {
		return this.translate(textToTranslate, null, targetLanguage);
	}

	@Override
	public String translate(String textToTranslate, String sourceLanguage, String targetLanguage)
			throws NodeException {
		sourceLanguage = mapLanguageCodeToTranslationLanguage(sourceLanguage);
		targetLanguage = mapLanguageCodeToTranslationLanguage(targetLanguage);

		TextResult translatedText;
		try {
			translatedText = translator.translateText(textToTranslate, sourceLanguage, targetLanguage);
		} catch (DeepLException | InterruptedException e) {
			throw new NodeException("Error occurred while translating text.", e);
		}

		return translatedText.getText();
	}

	private String mapLanguageCodeToTranslationLanguage(String languageCode) {
		final var mappedLanguage = (Map<String, Object>) this.languagePreferences.get(
				"language");

		if (mappedLanguage == null || mappedLanguage.get(languageCode) == null) {
			return languageCode;
		}

		final var postfix = (String) mappedLanguage.get(languageCode);

		return String.format("%s-%s", languageCode, postfix.toUpperCase());
	}

}
