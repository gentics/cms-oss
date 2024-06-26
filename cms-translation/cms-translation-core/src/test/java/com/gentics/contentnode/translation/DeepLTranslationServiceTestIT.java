package com.gentics.contentnode.translation;

import static com.gentics.contentnode.translation.TranslationTestUtils.mockTranslationConfig;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class DeepLTranslationServiceTestIT {

	final String TRANSLATION_PROVIDER = "deepl";
	final String API_TOKEN = "<api-key>";

	@Test
	@Disabled("Meant for manual execution")
	public void givenTranslationService_shouldTranslateText() throws Exception {
		try (MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class)) {
			mockedNodeRuntimeConfig.when(NodeConfigRuntimeConfiguration::getPreferences)
					.thenReturn(mockTranslationConfig(TRANSLATION_PROVIDER, API_TOKEN));

			var textToTranslate = """
					<!DOCTYPE html>
					<title>Seiten Titel</title>
					     
					<h1>Mein Blog ist toll</h1>
					<p>Ich habe einen deutschen Text, den ich übersetzen will</p>
					     
					""";

			var translatedText = TranslationUtil.translate(textToTranslate, "de", "en");
			assertThat(translatedText).isEqualTo("Hello world!");
		}
	}

}