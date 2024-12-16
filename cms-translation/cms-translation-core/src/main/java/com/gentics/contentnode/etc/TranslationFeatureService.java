package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.license.feature.LicenseKeyFeatureDefinition;

/**
 * Implementation of {@link FeatureService} which provides the
 * {@link Feature#AUTOMATIC_TRANSLATION}
 */
public class TranslationFeatureService implements FeatureService {

	@Override
	public boolean isProvided(Feature feature) {
		var isActivatedAndLicensed =
				feature == Feature.AUTOMATIC_TRANSLATION && LicenseService.isLicenseSufficient(
						LicenseKeyFeatureDefinition.TRANSL);
		if (!isActivatedAndLicensed) {
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
