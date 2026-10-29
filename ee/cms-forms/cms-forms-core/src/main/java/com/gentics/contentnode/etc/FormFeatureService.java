package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.license.feature.LicenseKeyFeatureDefinition;

/**
 * Implementation of {@link FeatureService} that provides the {@link Feature#FORMS}
 */
public class FormFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		switch (feature) {
		case FORMS:
			return LicenseService.isLicenseSufficient(LicenseKeyFeatureDefinition.FORMS) && FormConfigProvider.isConfigValid();
		default:
			return false;
		}
	}
}
