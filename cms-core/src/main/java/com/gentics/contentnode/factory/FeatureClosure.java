package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * AutoClosable Feature
 */
public class FeatureClosure implements AutoCloseable {
	/**
	 * Feature
	 */
	private Feature feature;

	/**
	 * Old value of the feature that will be used for the reset
	 */
	private boolean oldValue;

	/**
	 * Create an instance, that sets the given feature to the given value
	 * remembers the old one.
	 * @param feature feature to set/unset
	 * @param value true to set, false to unset
	 * @throws NodeException
	 */
	public FeatureClosure(Feature feature, boolean value) throws NodeException {
		this.feature = feature;
		oldValue = NodeConfigRuntimeConfiguration.isFeature(feature);
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setFeature(feature, value);
	}

	@Override
	public void close() {
		// Reset the original feature value
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setFeature(feature, oldValue);
	}
}
