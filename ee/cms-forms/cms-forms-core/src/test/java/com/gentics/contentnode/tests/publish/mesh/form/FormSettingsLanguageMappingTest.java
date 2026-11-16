package com.gentics.contentnode.tests.publish.mesh.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.forms.FormTransformer;
import com.gentics.contentnode.rest.model.formtype.TranslationSetting;

/**
 * Test cases for the language code mapping of translation values in form settings
 */
public class FormSettingsLanguageMappingTest {
	protected static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Create a form type configuration with translation form settings (one with propertyPath) and a language mapping
	 * @return configuration
	 */
	protected FormTypeConfiguration createConfig() {
		FormTypeConfiguration config = new FormTypeConfiguration(Map.of("en", "Test"), "forms");
		config.languageMapping = Map.of("de-AT", "de");

		TranslationSetting translation = new TranslationSetting("formTranslation", Map.of());
		TranslationSetting translationWithPath = new TranslationSetting("formTranslationWithPath", Map.of());
		translationWithPath.propertyPath = "custom.translation";
		config.settings = List.of(translation, translationWithPath);
		return config;
	}

	/**
	 * Create form data with values for the translation form settings, using the given code for the german translation
	 * @param germanCode language code of the german translation
	 * @return form data
	 */
	protected ObjectNode createData(String germanCode) {
		ObjectNode data = mapper.createObjectNode();
		data.withObjectProperty("formSettings").withObjectProperty("formTranslation").put(germanCode, "Hallo").put("en", "Hello");
		data.withObjectProperty("custom").withObjectProperty("translation").put(germanCode, "Servus").put("en", "Hi");
		return data;
	}

	@Test
	public void testToExternalLanguageCodes() throws NodeException {
		assertThat(FormTransformer.transformToExternalLanguageCodes(createConfig(), createData("de")))
				.as("Transformed data").isEqualTo(createData("de-AT"));
	}

	@Test
	public void testToInternalLanguageCodes() throws NodeException {
		assertThat(FormTransformer.transformToInternalLanguageCodes(createConfig(), createData("de-AT")))
				.as("Transformed data").isEqualTo(createData("de"));
	}
}
