package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Selectable option
 */
public class SelectOption implements Serializable {
	private static final long serialVersionUID = 8782870047754298238L;

	/**
	 * Option value
	 */
	@JsonProperty
	public String value;

	/**
	 * Label of the option which is displayed to the editor
	 */
	@JsonProperty
	public Map<String, String> labelI18n;

	/**
	 * Creator
	 * @param value value
	 * @param labelI18n label
	 */
	@JsonCreator
	public SelectOption(@JsonProperty(value = "value", required = true) String value,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		this.value = value;
		this.labelI18n = labelI18n;
	}
}
