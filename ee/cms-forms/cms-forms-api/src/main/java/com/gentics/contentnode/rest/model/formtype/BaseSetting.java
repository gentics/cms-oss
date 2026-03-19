package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME, 
		include = JsonTypeInfo.As.PROPERTY, 
		property = "type")
@JsonSubTypes({
	@Type(value = StringSetting.class, name = "string"),
	@Type(value = TranslationSetting.class, name = "translation"),
	@Type(value = IntegerSetting.class, name = "integer"),
	@Type(value = NumberSetting.class, name = "number"),
	@Type(value = BooleanSetting.class, name = "boolean"),
	@Type(value = DateSetting.class, name = "date"),
	@Type(value = SelectSetting.class, name = "select"),
	@Type(value = OptionsSetting.class, name = "options"),
	@Type(value = UserSetting.class, name = "user"),
	@Type(value = ReferenceSetting.class, name = "reference")
})
/**
 * Common properties for all setting types
 */
public abstract class BaseSetting implements Serializable {
	private static final long serialVersionUID = 861406133576553289L;

	/**
	 * The ID of the setting. Will be used to read the settings value in the backend or form-gen
	 */
	@JsonProperty
	public String id;

	/**
	 * Label of the setting which is displayed to the editor
	 */
	@JsonProperty
	public Map<String, String> labelI18n;

	/**
	 * Description of the setting for the editor
	 */
	@JsonProperty
	public Map<String, String> descriptionI18n;

	/**
	 * If the editor is required to fill out the setting
	 */
	@JsonProperty
	public boolean required;

	/**
	 * If this setting is only to be processed/used in the backend/mesh-plugin, and not to be forwarded to form-gen
	 */
	@JsonProperty
	public boolean backend;

	/**
	 * Creator
	 * @param id ID
	 * @param labelI18n label
	 */
	@JsonCreator
	public BaseSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		this.id = id;
		this.labelI18n = labelI18n;
	}
}
