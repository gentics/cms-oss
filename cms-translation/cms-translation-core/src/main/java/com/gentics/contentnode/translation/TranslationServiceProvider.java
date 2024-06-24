package com.gentics.contentnode.translation;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.runtime.ConfigurationValue;

/**
 * Acts as a factory to create a translation service instance based on the specified provider.
 */
public class TranslationServiceProvider {

	private TranslationServiceProvider() {
	}


	/**
	 * Builder to instantiate translation service.
	 */
	public static class Builder {

		/**
		 * The translation service to be instantiated
		 */
		private TranslationService translationService;

		/**
		 * Returns the instantiated translation service based on the given provider
		 * {@link #withProvider(SupportedProvider)} asserts that the translation service is properly
		 * configured.
		 *
		 * @return the translation service.
		 * @throws NodeException if instantiation was unsuccessfully
		 */
		public TranslationService build() throws NodeException {
			if (!(this.translationService instanceof TranslationService)) {
				throw new NodeException("Translation service is not an instance of TranslationService");
			}

			return this.translationService;
		}


		/**
		 * Reads the provider specified in the configuration and instantiates the corresponding
		 * translation service
		 *
		 * @return the translation service
		 * @throws NodeException if no valid provider is configured
		 */
		public Builder withProviderFromConfig() throws NodeException {
			var configuredProvider = ConfigurationValue.TRANSLATION_PROVIDER.get();

			if (configuredProvider == null) {
				throw new NodeException("Provider could not be read from configuration");
			}

			final var provider = SupportedProvider.fromString(configuredProvider);

			return withProvider(provider);
		}

		/**
		 * Instantiate the translation service corresponding to the given provider.
		 *
		 * @param provider the translation provider
		 * @return Fluent API
		 * @throws NodeException throws when no interface implementation for the given provider is found.
		 */
		public Builder withProvider(SupportedProvider provider)
				throws NodeException {
			switch (provider) {
				case DEEPL -> this.translationService = new DeepLTranslationService();
				default -> throw new NodeException(String.format("Unknown translation provider: '%s'", provider));
			}
			return this;
		}
	}

}
