package com.gentics.contentnode.rest.model.formtype;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration of a control
 */
public class ControlConfig extends ElementConfig {
	private static final long serialVersionUID = -6140165480405551895L;

	/**
	 * Aggregation allows a group of controls to be entered multiple times. The value will be aggregated into a list of objects
	 */
	@JsonProperty
	public boolean aggregate;

	/**
	 * List of control types which are allowed to be inserted into this container
	 */
	@JsonProperty
	public List<String> whitelist;

	/**
	 * Creator
	 * @param labelI18n label
	 */
	@JsonCreator
	public ControlConfig(@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		super(labelI18n);
	}
}
