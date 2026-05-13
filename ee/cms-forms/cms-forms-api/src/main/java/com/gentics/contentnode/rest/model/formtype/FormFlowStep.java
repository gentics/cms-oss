package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form flow step
 */
public class FormFlowStep implements Serializable {
	private static final long serialVersionUID = 1555072778957344550L;

	/**
	 * The translation key for the name of this flow step
	 */
	@JsonProperty
	public String nameTranslationKey;

	/**
	 * The translation key for the description of this flow step
	 */
	@JsonProperty
	public String descriptionTranslationKey;

	/**
	 * The translation key for the button label which leads to this flow step
	 */
	@JsonProperty
	public String buttonLabelTranslationKey;

	/**
	 * The React Native component class which renders this step
	 */
	@JsonProperty
	public String reactClass;

	/**
	 * Additional buttons which are displayed in the footer
	 */
	@JsonProperty
	public List<FormFlowButton> buttons;

	/**
	 * Variables which are passed to the screen component as props
	 */
	@JsonProperty
	public List<FormFlowVariable> variables;

	/**
	 * Json creator
	 * @param nameTranslationKey
	 * @param buttonLabelTranslationKey
	 * @param reactClass
	 */
	@JsonCreator
	public FormFlowStep(@JsonProperty(value = "nameTranslationKey", required = true) String nameTranslationKey,
			@JsonProperty(value = "buttonLabelTranslationKey", required = true) String buttonLabelTranslationKey,
			@JsonProperty(value = "reactClass", required = true) String reactClass) {
		this.nameTranslationKey = nameTranslationKey;
		this.buttonLabelTranslationKey = buttonLabelTranslationKey;
		this.reactClass = reactClass;
	}
}
