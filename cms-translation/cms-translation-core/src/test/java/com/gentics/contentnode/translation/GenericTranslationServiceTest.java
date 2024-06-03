package com.gentics.contentnode.translation;


import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class GenericTranslationServiceTest {

	TranslationService translationService;
	final String TRANSLATION_PROVIDER = "deepl";


	@Test
	public void givenSpecifiedTranslationService_shouldInstantiateCorrectClass()
			throws NodeException {
		translationService = new GenericTranslationService.Builder().withProvider(
				SupportedProvider.DEEPL).build();

		assertThat(translationService).isInstanceOf(DeepLTranslationService.class);
	}

	@Test
	public void givenConfiguredTranslationService_shouldInstantiateCorrectClass()
			throws NodeException {
		try (MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class)) {
			mockedNodeRuntimeConfig.when(() -> NodeConfigRuntimeConfiguration.getPreferences())
					.thenReturn(mockTranslationConfig(TRANSLATION_PROVIDER));

			var configuredProvider = ConfigurationValue.TRANSLATION_PROVIDER.get();
			assertThat(configuredProvider).isEqualTo(TRANSLATION_PROVIDER);

			translationService = new GenericTranslationService.Builder().withProviderFromConfig().build();
			assertThat(translationService).isInstanceOf(DeepLTranslationService.class);
		}
	}

	@Test
	public void givenMissingTranslationProvider_shouldThrowException() {
		assertThatThrownBy(() -> new GenericTranslationService.Builder().build());
		Throwable exception = catchThrowable(() -> new GenericTranslationService.Builder().build());

		assertThat(exception).isInstanceOf(NodeException.class);
		assertThat(exception).hasMessage(
				"Translation service is not an instance of TranslationService");
	}

	@Test
	public void givenInvalidConfiguredTranslationService_shouldThrowException() {
		try (MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class)) {
			mockedNodeRuntimeConfig.when(() -> NodeConfigRuntimeConfiguration.getPreferences())
					.thenReturn(mockTranslationConfig("not-existing-provider"));

			Throwable exception = catchThrowable(() -> new GenericTranslationService.Builder().withProviderFromConfig());

			assertThat(exception).isInstanceOf(NodeException.class);
			assertThat(exception).hasMessageContaining("Invalid provider value");
		}
	}


	private MapPreferences mockTranslationConfig(String provider) {
		Map<String, Object> mockedNodeRuntimeConfig = new HashMap<>();
		Map<String, Object> mockedTranslationConfig = new HashMap<>();

		mockedTranslationConfig.put("provider", provider);
		mockedNodeRuntimeConfig.put("translation", mockedTranslationConfig);

		return new MapPreferences(mockedNodeRuntimeConfig);
	}

}