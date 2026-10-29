package com.gentics.contentnode.etc;

/**
 * Implementation of {@link FeatureService} that provides the {@link Feature#ELASTICSEARCH}
 */
public class SearchFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.ELASTICSEARCH;
	}
}
