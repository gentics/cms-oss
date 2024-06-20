package com.gentics.contentnode.translation;


import static com.gentics.contentnode.translation.TranslationTestUtils.mockTranslationConfig;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Test class to verify that the translation service is instantiated properly.
 */
class GenericTranslationServiceTest {

	static final String TRANSLATION_PROVIDER = "deepl";
	static final String API_TOKEN = "api-key";
	private TranslationService translationService;


	@BeforeEach
	void mockSetting() {
		MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class);
		mockedNodeRuntimeConfig.when(() -> NodeConfigRuntimeConfiguration.getPreferences())
				.thenReturn(mockTranslationConfig(TRANSLATION_PROVIDER, API_TOKEN));
	}

	@AfterEach
	void tearDown() {
		Mockito.framework().clearInlineMocks();
	}


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
		var configuredProvider = ConfigurationValue.TRANSLATION_PROVIDER.get();
		assertThat(configuredProvider).isEqualTo(TRANSLATION_PROVIDER);

		translationService = new GenericTranslationService.Builder().withProviderFromConfig().build();
		assertThat(translationService).isInstanceOf(DeepLTranslationService.class);
	}

	@Test
	public void givenMissingTranslationProvider_shouldThrowException() {
		Throwable exception = catchThrowable(() -> new GenericTranslationService.Builder().build());

		assertThat(exception).isInstanceOf(NodeException.class);
		assertThat(exception).hasMessage(
				"Translation service is not an instance of TranslationService");
	}

	@Test
	public void givenInvalidConfiguredTranslationService_shouldThrowException() {
		Mockito.framework().clearInlineMocks();
		MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class);
		mockedNodeRuntimeConfig.when(() -> NodeConfigRuntimeConfiguration.getPreferences())
				.thenReturn(mockTranslationConfig("not-existing-provider", API_TOKEN));

		Throwable exception = catchThrowable(
				() -> new GenericTranslationService.Builder().withProviderFromConfig());

		assertThat(exception).isInstanceOf(NodeException.class);
		assertThat(exception).hasMessageContaining("Invalid provider value");
	}

}