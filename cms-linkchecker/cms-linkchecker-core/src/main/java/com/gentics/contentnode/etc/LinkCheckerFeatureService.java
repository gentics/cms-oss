package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicensedFeature;
import com.gentics.contentnode.LicenseService;

/**
 * Service that provides the {@link Feature#LINK_CHECKER}
 */
public class LinkCheckerFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.LINK_CHECKER && LicenseService.isLicenseSufficient(LicensedFeature.ELC);
	}

}
