package com.gentics.contentnode.translation;

import com.deepl.api.AuthorizationException;
import com.deepl.api.DeepLException;
import com.deepl.api.QuotaExceededException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.TranslationException;
import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.i18n.CNI18nString;
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

	public DeepLTranslationService(Translator translator) {
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
	public String translate(String textToTranslate, @Nullable String sourceLanguage,
			String targetLanguage)
			throws NodeException {
		var mappedTargetLanguage = mapLanguageCodeToTranslationLanguage(targetLanguage);

		TextResult translatedText;
		try {
			translatedText = translator.translateText(textToTranslate, sourceLanguage,
					mappedTargetLanguage);
		} catch (DeepLException exception) {
			var errorMessage = this.getErrorMessage(exception);
			throw new TranslationException(errorMessage.toString());
		} catch (InterruptedException e) {
			throw new NodeException("Error occurred while translating text.", e);
		}

		if (translatedText == null || translatedText.getText() == null) {
			return textToTranslate;
		}

		return translatedText.getText();
	}


	/**
	 * Generates an error message based on the type of DeepLException encountered.
	 *
	 * @param exception the DeepLException that was thrown
	 * @return a CNI18nString containing the error message with a specific reason
	 */
	private CNI18nString getErrorMessage(DeepLException exception) {
		var errorMessage = new CNI18nString("feature.translation.error");
		CNI18nString errorReason = new CNI18nString("feature.translation.error.reason_generic");

		if (exception instanceof QuotaExceededException) {
			errorReason = new CNI18nString("feature.translation.error.reason.quota_exceeded");
		} else if (exception instanceof AuthorizationException) {
			errorReason = new CNI18nString("feature.translation.error.reason.auth_failure");
		}
		errorMessage.addParameter(errorReason.toString());

		return errorMessage;
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
			throw new NodeException(
					"Invalid authentication token provided for: " + getClass().getSimpleName());
		}

		return authToken;
	}

	/**
	 * Reads the language settings from the configuration.
	 *
	 * @return The language settings
	 */
	private Map<String, Object> getLanguageSettingFromConfig() {
		var languagePreferencesFromConfig = ConfigurationValue.TRANSLATION_LANGUAGE_PREFERENCES.getMap();

		if (MapUtils.isEmpty(languagePreferencesFromConfig)) {
			final var errorMessage = String.format(
					"No language preferences found for: '%s'. Using default configuration.",
					getClass().getSimpleName());
			logger.log(Level.WARN, errorMessage);
		}

		return getDefaultLanguageConfig();
	}

	/**
	 * Map the given language code to a supported translation language. For example for the English
	 * language there are two flavors: en-US, en-GB
	 * <a href="https://developers.deepl.com/docs/v/de/resources/supported-languages">...</a>
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
