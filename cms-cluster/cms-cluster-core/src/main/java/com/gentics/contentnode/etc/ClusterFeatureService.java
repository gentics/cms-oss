package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.contentnode.LicensedFeature;

/**
 * Implementation of {@link FeatureService} which provides the {@link Feature#CLUSTER}
 */
public class ClusterFeatureService implements FeatureService {

	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.CLUSTER && LicenseService.isLicenseSufficient(LicensedFeature.CLUSTER);
	}
}
