package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.license.feature.LicenseKeyFeatureDefinition;

/**
 * Service that provides the {@link Feature#CONTENT_STAGING}
 */
public class ContentStagingFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.CONTENT_STAGING && LicenseService.isLicenseSufficient(LicenseKeyFeatureDefinition.CS);
	}
}
