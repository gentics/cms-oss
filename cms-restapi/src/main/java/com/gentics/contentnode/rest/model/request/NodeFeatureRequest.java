package com.gentics.contentnode.rest.model.request;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.NodeFeature;

/**
 * Request for activating, deactivating or setting node features
 */
@XmlRootElement
public class NodeFeatureRequest {

	/**
	 * Activated features
	 */
	protected List<NodeFeature> features;

	/**
	 * Create an empty request
	 */
	public NodeFeatureRequest() {}

	/**
	 * Create a request with the given list of features
	 * @param features list of features
	 */
	public NodeFeatureRequest(List<NodeFeature> features) {
		setFeatures(features);
	}

	/**
	 * Get the activated features
	 * @return activated features
	 */
	public List<NodeFeature> getFeatures() {
		return features;
	}

	/**
	 * Set the activated features
	 * @param features activated features
	 */
	public void setFeatures(List<NodeFeature> features) {
		this.features = features;
	}
}
