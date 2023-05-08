package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.NodeFeature;

/**
 * Response containing the activated features
 */
@XmlRootElement
public class NodeFeatureResponse extends GenericResponse {

	/**
	 * Activated features
	 */
	protected List<NodeFeature> features;

	/**
	 * Empty constructor
	 */
	public NodeFeatureResponse() {
		super();
	}

	/**
	 * Create an instance with empty message, but given response info and features
	 * @param responseInfo response info
	 * @param features features
	 */
	public NodeFeatureResponse(ResponseInfo responseInfo, List<NodeFeature> features) {
		super(null, responseInfo);
		setFeatures(features);
	}

	/**
	 * Create an instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public NodeFeatureResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
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
