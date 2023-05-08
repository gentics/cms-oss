package com.gentics.contentnode.etc;

/**
 * Interface for implementations that provide {@link Feature}s
 */
public interface FeatureService {
	/**
	 * Check whether the feature is provided
	 * @param feature feature to check
	 * @return true iff the feature is provided
	 */
	boolean isProvided(Feature feature);
}
