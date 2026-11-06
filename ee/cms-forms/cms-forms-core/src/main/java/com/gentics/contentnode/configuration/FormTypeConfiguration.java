package com.gentics.contentnode.configuration;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.contentnode.rest.model.formtype.FormGenTranslations;
import com.gentics.contentnode.rest.model.formtype.FormTypeConfigurationModel;

/**
 * Model of the form type configuration as deserialized from the CMS configuration
 */
@JsonIgnoreProperties({ "type" })
public class FormTypeConfiguration extends FormTypeConfigurationModel {
	private static final long serialVersionUID = 463767602343821275L;

	/**
	 * Optional plugin configuration (which is not exposed via REST API)
	 */
	@JsonProperty
	public ObjectNode pluginConfiguration;

	/**
	 * Form Type Configuration specific Translations for FormGen
	 */
	@JsonProperty
	public FormGenTranslations formGenTranslations;

	/**
	 * Form Type Configuration specific language code mapping
	 */
	@JsonProperty
	public Map<String, String> languageMapping;

	/**
	 * Creator
	 * @param nameI18n label
	 * @param pluginName plugin name
	 */
	@JsonCreator
	public FormTypeConfiguration(@JsonProperty(value = "nameI18n", required = true) Map<String, String> nameI18n,
			@JsonProperty(value = "pluginName", required = true) String pluginName) {
		this.nameI18n = nameI18n;
		this.pluginName = pluginName;
	}
}
