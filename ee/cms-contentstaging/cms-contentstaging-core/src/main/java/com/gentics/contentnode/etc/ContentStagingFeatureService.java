package com.gentics.contentnode.etc;

/**
 * Service that provides the {@link Feature#CONTENT_STAGING}
 */
public class ContentStagingFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.CONTENT_STAGING;
	}
}
