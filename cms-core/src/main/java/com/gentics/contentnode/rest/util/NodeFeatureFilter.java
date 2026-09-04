package com.gentics.contentnode.rest.util;

import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Filter for nodes that have a specific feature enabled
 */
public class NodeFeatureFilter extends AbstractNodeObjectFilter {
	/**
	 * Map containing all possible PermissionFilters
	 */
	protected static Map<Feature, NodeObjectFilter> filters = new HashMap<Feature, NodeObjectFilter>();

	/**
	 * Create all possible filters
	 */
	static {
		for (Feature feature : Feature.values()) {
			filters.put(feature, new NodeFeatureFilter(feature));
		}
	}

	/**
	 * Checked Feature
	 */
	protected Feature feature;

	/**
	 * Get the instance of the feature filter for the given feature
	 * @param feature checked feature
	 * @return filter
	 */
	public static NodeObjectFilter get(Feature feature) throws NodeException {
		return filters.get(feature);
	}

	/**
	 * Create an instance of the filter
	 * @param feature checked feature
	 */
	protected NodeFeatureFilter(Feature feature) {
		this.feature = feature;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.util.NodeObjectFilter#matches(com.gentics.lib.base.object.NodeObject)
	 */
	public boolean matches(NodeObject object) throws NodeException {
		if (object instanceof Node node) {
			return NodeConfigRuntimeConfiguration.isFeature(feature, node);
		} else {
			return false;
		}
	}
}
