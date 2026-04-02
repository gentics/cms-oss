package com.gentics.contentnode.publish.mesh;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * POJO for the forms plugin status response
 */
public class FormsPluginStatusResponse {
	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the forms-plugin is active")
	private Boolean active;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the forms-plugin data is removed from the search indexing")
	private Boolean noIndex;

	/**
	 * Check if the forms-plugin data is removed from the search indexing
	 * 
	 * @return
	 */
	public Boolean getNoIndex() {
		return noIndex;
	}

	/**
	 * Set the noIndex status
	 * 
	 * @param noIndex
	 */
	public void setNoIndex(Boolean noIndex) {
		this.noIndex = noIndex;
	}

	/**
	 * Flag which indicates whether the forms-plugin is active
	 * @return active flag
	 */
	public Boolean getActive() {
		return active;
	}

	/**
	 * Set active flag
	 * @param active flag
	 */
	public void setActive(Boolean active) {
		this.active = active;
	}
}
