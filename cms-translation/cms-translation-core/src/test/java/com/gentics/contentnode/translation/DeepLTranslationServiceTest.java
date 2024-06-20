package com.gentics.contentnode.translation;

import static com.gentics.contentnode.etc.JsoupValidator.VALIDATOR;
import static com.gentics.contentnode.translation.TranslationTestUtils.mockTranslationConfig;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.gentics.api.lib.exception.NodeException;
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
		final String TRANSLATION_PROVIDER = "deepl";

		MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class);
		mockedNodeRuntimeConfig.when(() -> NodeConfigRuntimeConfiguration.getPreferences())
				.thenReturn(mockTranslationConfig(TRANSLATION_PROVIDER, "<api-token>"));
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

	@Test
	public void givenCorruptMarkup_shouldThrowException() throws Exception {
		var mockedTranslator = Mockito.mock(Translator.class);
		when(mockedTranslator.translateText(anyString(), anyString(), anyString())).thenReturn(
				givenCorruptHtmlResult());

		this.translationService = new DeepLTranslationService(mockedTranslator);

		Exception exception = assertThrows(NodeException.class, () -> {
			translationService.translate("Hallo Welt!", "de", "fr", VALIDATOR);
		});

		final var expectedMessage = "Translated markup is invalid";
		final var actualMessage = exception.getMessage();

		assertThat(actualMessage).contains(expectedMessage);
	}


	private TextResult givenTextResult() {
		var result = new TextResult("Hello World", "de");
		return result;
	}

	private TextResult givenCorruptHtmlResult() {
		var result = new TextResult("<div>Hello World", "de");
		return result;
	}

}