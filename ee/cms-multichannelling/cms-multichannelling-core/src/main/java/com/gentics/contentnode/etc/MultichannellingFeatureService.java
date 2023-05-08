package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.contentnode.LicensedFeature;

/**
 * Service that provides {@link Feature#MULTICHANNELLING} and {@link Feature#MCCR}
 */
public class MultichannellingFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return (feature == Feature.MULTICHANNELLING && LicenseService.isLicenseSufficient(LicensedFeature.MC))
				|| (feature == Feature.MCCR  && LicenseService.isLicenseSufficient(LicensedFeature.MCCR));
	}
}
