package com.gentics.contentnode.translation;

import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.log.NodeLogger;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;

/**
 * Translation service for the DeepL translator.
 */
public class DeepLTranslationService implements TranslationService {

	/**
	 * The concrete translation provider
	 */
	static final SupportedProvider PROVIDER = SupportedProvider.DEEPL;
	/**
	 * The logger
	 */
	protected final NodeLogger logger = NodeLogger.getNodeLogger(getClass());
	/**
	 * The actual translation client
	 */
	private final Translator translator;
	/**
	 * The language preferences to map language codes to actual translation languages
	 */
	Map<String, Object> languagePreferences;


	public DeepLTranslationService() throws NodeException {
		final var authToken = getAuthTokenFromConfig();
		languagePreferences = getLanguageSettingFromConfig();
		translator = new Translator(authToken);
	}

	public DeepLTranslationService(Translator translator) throws NodeException {
		languagePreferences = getLanguageSettingFromConfig();
		this.translator = translator;
	}

	/**
	 * This method initializes a map with default language configurations.
	 *
	 * @return A Map containing language-region pairs.
	 */
	private static Map<String, Object> getDefaultLanguageConfig() {
		Map<String, Object> regionConfig = new HashMap<>();
		regionConfig.put("en", "US");
		regionConfig.put("pt", "BR");

		return regionConfig;
	}

	@Override
	public String translate(String textToTranslate, String targetLanguage) throws NodeException {
		return this.translate(textToTranslate, null, targetLanguage);
	}

	@Override
	public String translate(String textToTranslate, @Nullable String sourceLanguage,
			String targetLanguage)
			throws NodeException {
		targetLanguage = mapLanguageCodeToTranslationLanguage(targetLanguage);

		TextResult translatedText;
		try {
			translatedText = translator.translateText(textToTranslate, sourceLanguage, targetLanguage);
		} catch (DeepLException | InterruptedException e) {
			throw new NodeException("Error occurred while translating text.", e);
		}

		return translatedText.getText();
	}

	/**
	 * Reads the authentication token from the configuration.
	 *
	 * @return The token used to authenticate the deepL client.
	 * @throws NodeException if the token cannot be read or is empty.
	 */
	private String getAuthTokenFromConfig() throws NodeException {
		var authToken = ConfigurationValue.TRANSLATION_AUTH_TOKEN.get();

		if (StringUtils.isBlank(authToken)) {
			throw new NodeException("Invalid authentication token provided for: " + PROVIDER);
		}

		return authToken;
	}

	/**
	 * Reads the language settings from the configuration.
	 *
	 * @return The language settings
	 * @throws NodeException if the settings cannot be read or are empty.
	 */
	private Map<String, Object> getLanguageSettingFromConfig() {
		var languagePreferences = ConfigurationValue.TRANSLATION_LANGUAGE_PREFERENCES.getMap();

		if (MapUtils.isEmpty(languagePreferences)) {
			final var errorMessage = String.format(
					"No language preferences found for: '%s'. Using default configuration.", PROVIDER);
			logger.log(Level.WARN, errorMessage);
		}

		languagePreferences = getDefaultLanguageConfig();

		return languagePreferences;
	}

	/**
	 * Map the given language code to a supported translation language. For example for the English
	 * language there are two flavors: en-US, en-GB
	 * https://developers.deepl.com/docs/v/de/resources/supported-languages
	 *
	 * @param languageCode the international language code
	 * @return the Api compatible language
	 */
	private String mapLanguageCodeToTranslationLanguage(String languageCode) {
		final var mappedLanguage = (String) this.languagePreferences.get(languageCode);

		if (mappedLanguage == null) {
			return languageCode;
		}

		final var postfix = mappedLanguage.toUpperCase();

		return String.format("%s-%s", languageCode, postfix.toUpperCase());
	}

}
