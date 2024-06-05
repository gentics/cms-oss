package com.gentics.contentnode.translation;

import static com.gentics.contentnode.translation.TranslationTestUtils.mockTranslationConfig;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class DeepLTranslationServiceTestIT {

	final String TRANSLATION_PROVIDER = "deepl";
	final String API_TOKEN = "<real-api-key>";
	TranslationService translationService;

	@Test
	@Disabled("Meant for manual execution")
	public void givenTranslationService_shouldTranslateText() throws NodeException {
		try (MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class)) {
			mockedNodeRuntimeConfig.when(() -> NodeConfigRuntimeConfiguration.getPreferences())
					.thenReturn(mockTranslationConfig(TRANSLATION_PROVIDER, API_TOKEN));

			this.translationService = new GenericTranslationService.Builder()
					.withProviderFromConfig()
					.build();

			var translatedText = translationService.translate("Hallo Welt!", "de", "en");
			assertThat(translatedText).isEqualTo("Hello world!");
		}
	}

}