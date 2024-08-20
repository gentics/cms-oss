package com.gentics.contentnode.translation;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.log.NodeLogger;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

/**
 * Translation service for the DeepL translator.
 */
public class GoogleCloudTranslationService implements TranslationService {

	/**
	 * The logger
	 */
	protected final NodeLogger logger = NodeLogger.getNodeLogger(getClass());
	/**
	 * The actual translation client
	 */
	private final Translate translator;

	public GoogleCloudTranslationService() throws NodeException {
		final var authToken = getAuthTokenFromConfig();
		translator = TranslateOptions.newBuilder()
				.setApiKey(authToken)
				.build()
				.getService();
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

	@Override
	public String translate(String textToTranslate, @Nullable String sourceLanguage,
			String targetLanguage) {
		var translationResponse = translator.translate(textToTranslate, Translate.TranslateOption.sourceLanguage(sourceLanguage),
				Translate.TranslateOption.targetLanguage(targetLanguage));

		return translationResponse.getTranslatedText();
	}



}
