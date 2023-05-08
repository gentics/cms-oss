package com.gentics.contentnode.publish.mesh;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * POJO for the forms plugin render response
 */
public class FormsPluginRenderResponse {
	@JsonProperty(required = false)
	@JsonPropertyDescription("Rendered form")
	private String html;

	/**
	 * Rendered form
	 * @return html
	 */
	public String getHtml() {
		return html;
	}

	/**
	 * Set the rendered form
	 * @param html form
	 */
	public void setHtml(String html) {
		this.html = html;
	}
}
