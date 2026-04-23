package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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
	 * The label of the flow for the editor
	 */
	@JsonProperty
	public Map<String, String> labelI18n;

	/**
	 * The translation key to use to get the name of this flow in FormGen
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
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n,
			@JsonProperty(value = "nameTranslationKey", required = true) String nameTranslationKey,
			@JsonProperty(value = "steps", required = true) List<FormFlowStep> steps) {
		this.id = id;
		this.labelI18n = labelI18n;
		this.nameTranslationKey = nameTranslationKey;
		this.steps = steps;
	}
}
