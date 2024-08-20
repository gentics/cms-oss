package com.gentics.contentnode.translation;

import static com.gentics.contentnode.translation.ConfigurationTestUtils.mockTranslationConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.etc.JsoupValidator;
import com.gentics.contentnode.etc.Validator;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class DeepLTranslationServiceTestIT {

	static final String IMPLEMENTATION_CLASS = "com.gentics.contentnode.translation.DeepLTranslationService";
	static final String API_TOKEN = "<api-token>";

	@Test
	@Disabled("Meant for manual execution")
	public void givenTranslationService_shouldTranslateTextAndReturnValidMarkup() throws Exception {
		try (MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class)) {
			mockedNodeRuntimeConfig.when(NodeConfigRuntimeConfiguration::getPreferences)
					.thenReturn(mockTranslationConfig(IMPLEMENTATION_CLASS, API_TOKEN));

			var textToTranslate = """
					<!DOCTYPE html>
					<title>Seiten Titel</title>
					<h1>Mein Blog ist toll</h1>
					<p>Ich habe einen deutschen Text, den ich übersetzen will</p>
					""";

			var translationService = getTranslationService();
			var translatedText = translationService.translate(textToTranslate, "de", "en");
			Validator validator = new JsoupValidator();
			assertThat(validator.validateAndCollectErrors(translatedText)).isEmpty();
		}
	}

	private TranslationService getTranslationService() {
		TranslationUtil.createOrUpdateServiceDefinitionFile();
		TranslationUtil.reloadService();
		return TranslationUtil.getTranslationService().get();
	}

}