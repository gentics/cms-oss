package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.contentnode.LicensedFeature;

/**
 * Implementation of {@link FeatureService} that provides the {@link Feature#FORMS}
 */
public class FormFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.FORMS && LicenseService.isLicenseSufficient(LicensedFeature.FORMS);
	}
}
