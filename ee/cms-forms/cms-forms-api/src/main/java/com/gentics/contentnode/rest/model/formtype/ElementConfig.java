package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for a single element
 */
public abstract class ElementConfig implements Serializable {
	private static final long serialVersionUID = -6869900846127326764L;

	/**
	 * The label for the element for the editor
	 */
	@JsonProperty
	public Map<String, String> labelI18n;

	/**
	 * A description of the element for the editor
	 */
	@JsonProperty
	public Map<String, String> descriptionI18n;

	/**
	 * The settings which an editor can configure when inserting this element
	 */
	@JsonProperty
	public List<BaseSetting> settings;

	/**
	 * Creator
	 * @param labelI18n label
	 */
	@JsonCreator
	public ElementConfig(@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		this.labelI18n = labelI18n;
	}
}
