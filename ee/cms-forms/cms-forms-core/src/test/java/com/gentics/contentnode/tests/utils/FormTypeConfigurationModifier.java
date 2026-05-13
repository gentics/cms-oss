package com.gentics.contentnode.tests.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.forms.Migration;
import com.gentics.contentnode.rest.model.formtype.ControlConfig;
import com.gentics.contentnode.rest.model.formtype.FormGenTranslations;
import com.gentics.contentnode.rest.model.formtype.StringSetting;
import com.gentics.contentnode.testutils.ConfigModifier;

/**
 * Implementation of {@link ConfigModifier} that adds a single form type configuration with key {@link #GENERIC_TYPE}.
 */
public class FormTypeConfigurationModifier implements ConfigModifier {
	/**
	 * Key of the injected form type configuration
	 */
	public final static String GENERIC_TYPE = "generic";

	public final static String ANDP_TYPE = "andp";

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		ObjectMapper mapper = new ObjectMapper();
		YAMLMapper yamlMapper = new YAMLMapper();

		FormTypeConfigurationMap formTypeConfiguration = new FormTypeConfigurationMap();

		// prepare forms config
		FormTypeConfiguration formsConfig = new FormTypeConfiguration(Map.of("de", "CMS Formular", "en", "CMS Form"),
				"forms");
		ControlConfig stringControl = new ControlConfig(Map.of("de", "Eingabefeld"));

		StringSetting stringSetting = new StringSetting("rich", Map.of("de", "Reicher Text"));
		stringSetting.backend = true;
		stringSetting.richContent = true;
		stringControl.settings = Arrays.asList(stringSetting);

		formsConfig.controls = Map.of("string", stringControl);

		try {
			formsConfig.pluginConfiguration = mapper.readValue("{\"password\": \"supersecret\"}", ObjectNode.class);
		} catch (JsonProcessingException e) {
			throw new NodeException(e);
		}
		formTypeConfiguration.put(GENERIC_TYPE, formsConfig);

		// prepare andp config
		FormTypeConfiguration andpConfig = new FormTypeConfiguration(Map.of("de", "ANDP Formular", "en", "ANDP Form"), "acta-nova");
		andpConfig.external = true;
		try {
			andpConfig.pluginConfiguration = mapper.readValue("{\"password\": \"supersecret\"}", ObjectNode.class);
		} catch (JsonProcessingException e) {
			throw new NodeException(e);
		}
		formTypeConfiguration.put(ANDP_TYPE, andpConfig);

		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfiguration);

		// add default translations
		try (InputStream in = Migration.class.getResourceAsStream(Migration.FORMGEN_TRANSLATIONS_FILENAME)) {
			prefs.set(FormConfigProvider.TRANSLATIONS_CONFIG_PATH, yamlMapper.readValue(in, FormGenTranslations.class));
		} catch (IOException e) {
			throw new NodeException("Error while reading translations", e);
		}
	}
}
