package com.gentics.contentnode.etc;

/**
 * Implementation of {@link FeatureService} which provides the
 * {@link Feature#AUTOMATIC_TRANSLATION}
 */
public class TranslationFeatureService implements FeatureService {

	@Override
	public boolean isProvided(Feature feature) {
		if (feature != Feature.AUTOMATIC_TRANSLATION) {
			return false;
		}
		var isCreatedOrUpdated = TranslationServiceLoaderUtil.createOrUpdateServiceDefinitionFile();
		if (!isCreatedOrUpdated) {
			return false;
		}

		if (TranslationServiceLoaderUtil.isAlreadyProvided()) {
			return true;
		}

		return TranslationServiceLoaderUtil.reloadService();
	}

}
