package com.gentics.contentnode.etc;

/**
 * Implementation of {@link FeatureService} which provides the {@link Feature#CLUSTER}
 */
public class ClusterFeatureService implements FeatureService {

	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.CLUSTER;
	}
}
