package com.gentics.contentnode.rest.model.formtype;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Size;

/**
 * Select where one or more options can be selected
 */
public class SelectSetting extends BaseSetting {
	private static final long serialVersionUID = -91439466760302592L;

	/**
	 * Selectable options
	 */
	@JsonProperty
	public List<SelectOption> options;

	/**
	 * If multiple values can be selected
	 */
	@JsonProperty
	public boolean multiple;

	/**
	 * Creator
	 * @param id ID
	 * @param labelI18n label
	 * @param options options
	 */
	@JsonCreator
	public SelectSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n,
			@JsonProperty(value = "options", required = true) @Size(min = 1) List<SelectOption> options) {
		super(id, labelI18n);
		this.options = options;
	}
}
