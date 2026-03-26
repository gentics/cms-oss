package com.gentics.contentnode.tests.utils;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.testutils.ConfigModifier;

/**
 * Implementation of {@link ConfigModifier} that adds a single form type configuration with key {@link #GENERIC_TYPE}.
 */
public class FormTypeConfigurationModifier implements ConfigModifier {
	/**
	 * Key of the injected form type configuration
	 */
	public final static String GENERIC_TYPE = "generic";

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		ObjectMapper mapper = new ObjectMapper();

		FormTypeConfigurationMap formTypeConfiguration = new FormTypeConfigurationMap();
		FormTypeConfiguration config = new FormTypeConfiguration(Map.of("de", "CMS Formulare", "en", "CMS Forms"),
				"forms");
		try {
			config.pluginConfiguration = mapper.readValue("{\"password\": \"supersecret\"}", ObjectNode.class);
		} catch (JsonProcessingException e) {
			throw new NodeException(e);
		}
		formTypeConfiguration.put(GENERIC_TYPE, config);
		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfiguration);
	}
}
