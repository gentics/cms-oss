package com.gentics.contentnode.translation;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
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


	static public String translate(String textToTranslate, String language) throws NodeException {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.TRANSLATION)) {
			return null;
		}
		return loader.execForFirst(service -> service.translate(textToTranslate, language)).orElse(StringUtils.EMPTY);
	}
}
