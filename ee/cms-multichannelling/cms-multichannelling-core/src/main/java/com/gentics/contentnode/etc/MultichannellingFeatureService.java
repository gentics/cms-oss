package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.license.feature.LicenseKeyFeatureDefinition;

/**
 * Service that provides {@link Feature#MULTICHANNELLING} and {@link Feature#MCCR}
 */
public class MultichannellingFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return (feature == Feature.MULTICHANNELLING && LicenseService.isLicenseSufficient(LicenseKeyFeatureDefinition.MC))
				|| (feature == Feature.MCCR  && LicenseService.isLicenseSufficient(LicenseKeyFeatureDefinition.MCCR));
	}
}
