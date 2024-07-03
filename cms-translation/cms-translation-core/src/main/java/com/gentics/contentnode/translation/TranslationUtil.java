package com.gentics.contentnode.translation;

import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class, which acts as bridge to the optional translation module
 */
public class TranslationUtil {

	/**
	 * {@link TranslationService} service
	 */
	private final static ServiceLoaderUtil<TranslationService> loader = ServiceLoaderUtil.load(
			TranslationService.class);

	public static Optional<TranslationService> getTranslationService() {
		return Optional.of(loader.iterator().next());
	}

	static public String translate(String textToTranslate, String sourceLanguage,
			String targetLanguage) throws Exception {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.TRANSLATION)) {
			return null;
		}
		return loader.execForFirst(service -> {
			try {
				return service.translate(textToTranslate, sourceLanguage, targetLanguage);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).orElse(StringUtils.EMPTY);
	}
}
