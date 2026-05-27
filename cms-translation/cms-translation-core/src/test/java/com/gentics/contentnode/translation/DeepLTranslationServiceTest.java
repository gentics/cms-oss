package com.gentics.contentnode.translation;

import static com.gentics.contentnode.translation.ConfigurationTestUtils.mockTranslationConfig;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class DeepLTranslationServiceTest {

	DeepLTranslationService translationService;

	@BeforeEach
	void setUp() {
		final String TRANSLATION_IMPLEMENTATION = "com.gentics.contentnode.api.translation.DeepLTranslationService";

		MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class);
		mockedNodeRuntimeConfig.when(NodeConfigRuntimeConfiguration::getPreferences)
				.thenReturn(mockTranslationConfig(TRANSLATION_IMPLEMENTATION, "<api-token>"));
	}

	@AfterEach
	void tearDown() {
		Mockito.framework().clearInlineMocks();
	}

	@Test
	public void givenTargetLanguageCode_shouldMapToApiCompatibleLanguage() throws Exception {
		var mockedTranslator = Mockito.mock(Translator.class);
		when(mockedTranslator.translateText(anyString(), anyString(), anyString())).thenReturn(
				givenTextResult());

		this.translationService = new DeepLTranslationService(mockedTranslator);
		translationService.translate("Hallo Welt!", "de", "en");

		verify(mockedTranslator).translateText(anyString(), eq("de"), eq("en-US"));
	}

	@Test
	public void givenNonExistingMappingTargetLanguage_shouldUseDefaultLanguageCode() throws Exception {
		var mockedTranslator = Mockito.mock(Translator.class);
		when(mockedTranslator.translateText(anyString(), anyString(), anyString())).thenReturn(
				givenTextResult());

		this.translationService = new DeepLTranslationService(mockedTranslator);
		translationService.translate("Hallo Welt!", "de", "fr");

		verify(mockedTranslator).translateText(anyString(), eq("de"), eq("fr"));
	}

	private TextResult givenTextResult() {
		return new TextResult("Hello World", "de", 0, null);
	}

	private TextResult givenCorruptHtmlResult() {
		return new TextResult("<div>Hello World", "de", 0, null);
	}

}