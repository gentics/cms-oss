package com.gentics.contentnode.publish.mesh;

import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for forms plugin languages
 */
public class FormsPluginLanguages implements RestModel {
	private List<String> languages;

	/**
	 * List of language codes for rendering fallback
	 * @return list of codes
	 */
	public List<String> getLanguages() {
		return languages;
	}

	/**
	 * Set the list of language codes for the rendering fallback
	 * @param languages list of codes
	 * @return fluent API
	 */
	public FormsPluginLanguages setLanguages(List<String> languages) {
		this.languages = languages;
		return this;
	}
}
