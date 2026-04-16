package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form Flow variable
 */
public class FormFlowVariable implements Serializable {
	private static final long serialVersionUID = 1575761202164022697L;

	/**
	 * Name of the variable
	 */
	@JsonProperty
	public String name;

	/**
	 * Value
	 */
	@JsonProperty
	public Object value;

	/**
	 * Json creator
	 * @param name
	 * @param value
	 */
	@JsonCreator
	public FormFlowVariable(@JsonProperty(value = "name", required = true) String name,
			@JsonProperty(value = "value", required = true) Object value) {
		this.name = name;
		this.value = value;
	}
}
