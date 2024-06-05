package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.contentnode.LicensedFeature;

/**
 * Implementation of {@link FeatureService} which provides the {@link Feature#TRANSLATION}
 */
public class TranslationFeatureService implements FeatureService {

	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.TRANSLATION && LicenseService.isLicenseSufficient(LicensedFeature.TRANSL);
	}
}
