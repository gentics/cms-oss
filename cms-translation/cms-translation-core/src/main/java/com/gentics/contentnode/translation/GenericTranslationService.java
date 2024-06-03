package com.gentics.contentnode.translation;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.runtime.ConfigurationValue;

/**
 * Acts as a factory to create a translation service instance based on the specified provider.
 */
public class GenericTranslationService implements TranslationService {

	/**
	 * The translation service
	 */
	TranslationService translationService;

	private GenericTranslationService() {
	}

	@Override
	public String translate(String textToTranslate, String targetLanguage) throws NodeException {
		return this.translationService.translate(textToTranslate, targetLanguage);
	}


	/**
	 * Builder to instantiate translation service.
	 */
	public static class Builder {

		/**
		 * The translation service to be instantiated
		 */
		TranslationService translationService;

		/**
		 * Returns the instantiated translation service based on the given provider
		 * {@link #withProvider(SupportedProvider)}  . Asserts that the translation service is properly
		 * configured.
		 *
		 * @return the translation service.
		 * @throws NodeException
		 */
		public TranslationService build() throws NodeException {
			if (this.translationService == null
					|| !(this.translationService instanceof TranslationService)) {
				throw new NodeException("Translation service is not an instance of TranslationService");
			}

			return this.translationService;
		}


		/**
		 * Reads the provider specified in the configuration and instantiates the corresponding
		 * translation service
		 *
		 * @return the translation service
		 * @throws NodeException
		 */
		public Builder withProviderFromConfig() throws NodeException {
			var configuredProvider = ConfigurationValue.TRANSLATION_PROVIDER.get();

			if (configuredProvider == null) {
				throw new NodeException("Provider could not be read from configuration");
			}

			var provider = SupportedProvider.fromString(configuredProvider);

			return withProvider(provider);
		}

		/**
		 * Specifies the translation provider
		 *
		 * @param provider the translation provider
		 * @return Fluent API
		 * @throws NodeException
		 */
		public Builder withProvider(SupportedProvider provider)
				throws NodeException {
			switch (provider) {
				case DEEPL -> this.translationService = new DeepLTranslationService();
			}
			return this;
		}
	}

}
