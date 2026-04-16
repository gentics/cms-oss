package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form flow button
 */
public class FormFlowButton implements Serializable {
	private static final long serialVersionUID = -7897748014729058101L;

	/**
	 * The React Native component class which renders this button
	 */
	@JsonProperty
	public String reactClass;

	/**
	 * The translation key for the label of this button
	 */
	@JsonProperty
	public String labelTranslationKey;

	/**
	 * Hides the button on mobile devices (if this button implementation has this feature)
	 */
	@JsonProperty
	public boolean mobileDisabled;

	/**
	 * Json creator
	 * @param reactClass
	 * @param labelTranslationKey
	 */
	@JsonCreator
	public FormFlowButton(@JsonProperty(value = "reactClass", required = true) String reactClass,
			@JsonProperty(value = "labelTranslationKey", required = true) String labelTranslationKey) {
		this.reactClass = reactClass;
		this.labelTranslationKey = labelTranslationKey;
	}
}
