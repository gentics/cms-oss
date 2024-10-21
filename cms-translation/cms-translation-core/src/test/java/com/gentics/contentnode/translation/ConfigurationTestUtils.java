package com.gentics.contentnode.translation;

import com.gentics.contentnode.etc.MapPreferences;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationTestUtils {

	public static MapPreferences mockTranslationConfig(String implementationClass, String apiToken) {
		Map<String, Object> nodeRuntimeConfig = new HashMap<>();
		Map<String, Object> translationConfig = new HashMap<>();

		translationConfig.put("implementation", implementationClass);
		translationConfig.put("auth", mockAuthConfig(apiToken));
		translationConfig.put("language", mockLanguageConfig());

		nodeRuntimeConfig.put("automatic_translation", translationConfig);

		return new MapPreferences(nodeRuntimeConfig);
	}

	private static Map<String, Object> mockAuthConfig(String apiToken) {
		Map<String, Object> authConfig = new HashMap<>();
		authConfig.put("token", apiToken);

		return authConfig;
	}

	private static Map<String, Object> mockLanguageConfig() {
		Map<String, Object> regionConfig = new HashMap<>();
		regionConfig.put("en", "US");

		return regionConfig;
	}
}
