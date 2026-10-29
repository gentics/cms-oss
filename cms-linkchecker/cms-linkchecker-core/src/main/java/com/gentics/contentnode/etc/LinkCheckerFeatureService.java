package com.gentics.contentnode.etc;

/**
 * Service that provides the {@link Feature#LINK_CHECKER}
 */
public class LinkCheckerFeatureService implements FeatureService {
	@Override
	public boolean isProvided(Feature feature) {
		return feature == Feature.LINK_CHECKER;
	}

}
