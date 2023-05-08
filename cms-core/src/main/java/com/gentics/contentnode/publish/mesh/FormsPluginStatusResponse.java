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
