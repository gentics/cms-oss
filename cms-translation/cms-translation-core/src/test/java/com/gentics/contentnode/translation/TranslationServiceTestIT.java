package com.gentics.contentnode.translation;

import static com.gentics.contentnode.translation.ConfigurationTestUtils.mockTranslationConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.etc.JsoupValidator;
import com.gentics.contentnode.etc.TranslationServiceLoaderUtil;
import com.gentics.contentnode.etc.TranslationStats;
import com.gentics.contentnode.etc.Validator;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

@Ignore("Meant for manual execution")
@RunWith(value = Parameterized.class)
public class TranslationServiceTestIT {

	static final String IMPLEMENTATION_CLASS = "com.gentics.contentnode.translation.DeepLTranslationService";
	//static final String IMPLEMENTATION_CLASS = "com.gentics.contentnode.translation.TranslateLocallyService";
	static final String API_TOKEN = "<api-key>";

	@Parameters(name = "{index}: text {0}")
	public static Collection<Object[]> data() {
		return List.of(
				new Object[] { "Ein wunderschönes Text zum Übersetzen" },
				new Object[] { """
				       <!DOCTYPE html>
				       <title>Seiten Titel</title>
				       <h1>Mein Blog ist toll</h1>
				       <p>Ich habe einen deutschen Text, den ich übersetzen will</p>
					""" },
				new Object[] { "" },
				new Object[] { "<p>Am <b>17. März</b> organisierte das <b>ADVANTAGE AUSTRIA Budapest</b> in Zusammenarbeit mit der <b>Austrian Business Agency (ABA)</b>, <b>Ecoplus – Niederösterreichs Wirtschaftsagentur</b>, der <b>Vienna Business Agency</b> sowie <b>BABEG – Success in Carinthia</b> eine gut besuchte Standortveranstaltung in den Räumlichkeiten der <b>Raiffeisen Bank Hungary</b>. Ziel des Events war es, ungarischen Unternehmen die Vorteile einer Investition in Österreich kompakt und praxisnah zu vermitteln.</p><p>Die rund <b>60 Teilnehmenden</b> erhielten einen fundierten Überblick über die wichtigsten <b>rechtlichen und steuerlichen Unterschiede</b> zwischen Österreich und Ungarn. Ergänzend dazu boten die Expertinnen und Experten konkrete Hinweise und Empfehlungen, wie ein erfolgreicher Markteintritt oder eine Standorterweiterung in Österreich gelingen kann.</p>" }
		);
	}

	@Parameter(0)
	public String textToTranslate;

	@Test
	public void givenTranslationService_shouldTranslateTextAndReturnValidMarkup() throws Exception {
		try (MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class)) {
			mockedNodeRuntimeConfig.when(NodeConfigRuntimeConfiguration::getPreferences)
					.thenReturn(mockTranslationConfig(IMPLEMENTATION_CLASS, API_TOKEN));

			var translationService = getTranslationService();
			var translatedText = translationService.translate(textToTranslate, "de", "en");
			Validator validator = new JsoupValidator();
			assertThat(validator.validateAndCollectErrors(translatedText)).isEmpty();
		}
	}

	@Test
	public void givenTagResolveService_shouldTranslateTextAndReturnValidMarkup() throws Exception {
		try (MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
				NodeConfigRuntimeConfiguration.class)) {
			mockedNodeRuntimeConfig.when(NodeConfigRuntimeConfiguration::getPreferences)
					.thenReturn(mockTranslationConfig(IMPLEMENTATION_CLASS, API_TOKEN));
			DummyTagResolveService tagResolveService = new DummyTagResolveService();
			TranslationService translationService = getTranslationService();

			var translatedText = tagResolveService.resolveAndTranslateTextContent(textToTranslate, List.of(), translationService, "de", "en", null);
			Validator validator = new JsoupValidator();
			assertThat(validator.validateAndCollectErrors(translatedText)).isEmpty();
		}
	}

	private TranslationService getTranslationService() {
		TranslationServiceLoaderUtil.createOrUpdateServiceDefinitionFile();
		TranslationServiceLoaderUtil.reloadService();
		return TranslationServiceLoaderUtil.getTranslationService().get();
	}

	private class DummyTagResolveService extends TagResolveService {

		protected DummyTagResolveService() throws NodeException {
			super(0);
		}

		@Override
		public String resolveAndTranslateTextContent(String resolvableText, List<Tag> contentTags,
				TranslationService translationService, String sourceLanguageCode, String targetLanguageCode,
				TranslationStats stats) throws Exception {
			return super.resolveAndTranslateTextContent(resolvableText, contentTags, translationService, sourceLanguageCode,
					targetLanguageCode, stats);
		}
	}
}