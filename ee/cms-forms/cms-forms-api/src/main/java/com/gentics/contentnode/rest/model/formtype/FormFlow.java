package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Definition of a single Form-Flow
 */
public class FormFlow implements Serializable {
	private static final long serialVersionUID = -651658151080239248L;

	/**
	 * The ID of the flow
	 */
	@JsonProperty
	public String id;

	/**
	 * The translation key to use to get the name of this flow
	 */
	@JsonProperty
	public String nameTranslationKey;

	/**
	 * Steps in the form flow
	 */
	@JsonProperty
	public List<FormFlowStep> steps;

	/**
	 * Json Creator
	 * @param id
	 * @param nameTranslationKey
	 * @param steps
	 */
	@JsonCreator
	public FormFlow(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "nameTranslationKey", required = true) String nameTranslationKey,
			@JsonProperty(value = "steps", required = true) List<FormFlowStep> steps) {
		this.id = id;
		this.nameTranslationKey = nameTranslationKey;
		this.steps = steps;
	}
}
