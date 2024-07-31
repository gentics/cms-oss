package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.contentnode.LicensedFeature;
import com.gentics.contentnode.translation.TranslationUtil;

/**
 * Implementation of {@link FeatureService} which provides the
 * {@link Feature#AUTOMATIC_TRANSLATION}
 */
public class TranslationFeatureService implements FeatureService {

	@Override
	public boolean isProvided(Feature feature) {
		var isActivatedAndLicensed =
				feature == Feature.AUTOMATIC_TRANSLATION && LicenseService.isLicenseSufficient(
						LicensedFeature.TRANSL);
		if (!isActivatedAndLicensed) {
			return false;
		}
		var isCreatedOrUpdated = TranslationUtil.createOrUpdateServiceDefinitionFile();
		if (!isCreatedOrUpdated) {
			return false;
		}

		return TranslationUtil.reloadService();
	}

}
