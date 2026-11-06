package com.gentics.contentnode.etc;

/**
 * Implementation of {@link FeatureService} which provides the {@link Feature#KEYCLOAK}
 */
public class KeycloakFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.KEYCLOAK;
	}
}
