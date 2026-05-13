package com.gentics.contentnode.publish.mesh;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Model of the formGen translations, which are sent to the plugins
 */
public class FormGenTranslationsModel implements RestModel, Serializable {
	private static final long serialVersionUID = -4554888889672007985L;

	/**
	 * Default translations
	 */
	@JsonProperty("default")
	public TranslationsModel _default;

	/**
	 * Type specific translations
	 */
	@JsonProperty("types")
	public Map<String, TranslationsModel> types;
}
