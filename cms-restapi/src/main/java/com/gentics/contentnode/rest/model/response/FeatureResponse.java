package com.gentics.contentnode.rest.model.response;

/**
 * Response containing information about a feature
 */
public class FeatureResponse extends GenericResponse {
	/**
	 * Feature name
	 */
	protected String name;

	/**
	 * Flag for feature activation
	 */
	protected boolean activated;

	/**
	 * Empty constructor
	 */
	public FeatureResponse() {
		super();
	}

	/**
	 * Create an instance with empty message, but given response info and features
	 * @param responseInfo response info
	 * @param features features
	 */
	public FeatureResponse(ResponseInfo responseInfo, String name, boolean activated) {
		super(null, responseInfo);
		setName(name);
		setActivated(activated);
	}

	/**
	 * Create an instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public FeatureResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Name of the feature
	 * @documentationExample new_tageditor
	 * @return feature name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the feature name
	 * @param name name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * True when the feature is activated, false if not
	 * @documentationExample true
	 * @return true for activated feature
	 */
	public boolean isActivated() {
		return activated;
	}

	/**
	 * Set whether the feature is activated
	 * @param activated true for activated feature
	 */
	public void setActivated(boolean activated) {
		this.activated = activated;
	}
}
