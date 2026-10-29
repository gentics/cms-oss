package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.license.feature.LicenseKeyFeatureDefinition;

/**
 * Implementation of {@link FeatureService} that provides the {@link Feature#ELASTICSEARCH}
 */
public class SearchFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.ELASTICSEARCH && LicenseService.isLicenseSufficient(LicenseKeyFeatureDefinition.ES);
	}
}
