package com.gentics.contentnode.translation;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.TranslationException;
import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.etc.TranslationConfigUtil;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import javax.annotation.Nullable;

/**
 * Translation service for the Basic Google Cloud Translator.
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
		logger.info("Instantiating Google Cloud Translation Service");
		final var authToken = TranslationConfigUtil.getAuthTokenFromConfig(getClass().getSimpleName());
		translator = TranslateOptions.newBuilder()
				.setApiKey(authToken)
				.build()
				.getService();
	}

	@Override
	public String translate(String textToTranslate, @Nullable String sourceLanguage,
			String targetLanguage) throws NodeException {
		try {
			var translationResponse = translator.translate(textToTranslate,
					Translate.TranslateOption.sourceLanguage(sourceLanguage),
					Translate.TranslateOption.targetLanguage(targetLanguage));

			return translationResponse.getTranslatedText();
		} catch (TranslateException e) {
			var errorMessage = new CNI18nString("feature.translation.error");
			CNI18nString errorReason = new CNI18nString("feature.translation.error.reason_generic");

			if (400 <= e.getCode() && e.getCode() < 500) {
				errorReason = new CNI18nString("feature.translation.error.reason_generic_client");
			}
			errorMessage.addParameter(errorReason.toString());

			this.logger.error("Error occurred while translating text", e);
			throw new TranslationException(errorMessage.toString());
		} catch (Exception e) {
			throw new NodeException("Error occurred while translating text.", e);
		}
	}

}
