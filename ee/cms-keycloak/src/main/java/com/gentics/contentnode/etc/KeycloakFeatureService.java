package com.gentics.contentnode.etc;

import com.gentics.contentnode.LicenseService;
import com.gentics.contentnode.LicensedFeature;

/**
 * Implementation of {@link FeatureService} which provides the {@link Feature#KEYCLOAK}
 */
public class KeycloakFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.KEYCLOAK && LicenseService.isLicenseSufficient(LicensedFeature.KC);
	}
}
