package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * AutoClosable for node features.
 */
public class NodeFeatureClosure implements AutoCloseable {

	/**
	 * Feature
	 */
	private final Feature feature;

	/**
	 * Node
	 */
	private final Node node;

	/**
	 * Old value of the feature that will be used for the reset
	 */
	private final boolean oldValue;

	/**
	 * Create an instance, that sets the given feature to the given value
	 * remembers the old one.
	 * @param feature feature to set/unset
	 * @param node Node to activate/deactivate the feature for
	 * @param value true to set, false to unset
	 */
	public NodeFeatureClosure(Feature feature, Node node, boolean value) throws NodeException {
		this.feature = feature;
		this.node = node;
		this.oldValue = NodeConfigRuntimeConfiguration.isFeature(feature);

		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setFeature(feature, value);

		if (node != null) {
			if (value) {
				node.activateFeature(feature);
			} else {
				node.deactivateFeature(feature);
			}
		}
	}

	@Override
	public void close() throws NodeException {
		// Reset the original feature value
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setFeature(feature, oldValue);

		if (node != null) {
			if (oldValue) {
				node.activateFeature(feature);
			} else {
				node.deactivateFeature(feature);
			}
		}
	}
}
