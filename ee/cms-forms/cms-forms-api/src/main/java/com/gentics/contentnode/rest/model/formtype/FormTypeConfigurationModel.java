package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The configuration for a single form type
 */
@JsonIgnoreProperties({"pluginConfiguration", "formGenTranslations", "languageMapping"})
public class FormTypeConfigurationModel implements Serializable {
	private static final long serialVersionUID = -8486198177336031697L;

	/**
	 * Type key
	 */
	@JsonProperty
	public String type;

	/**
	 * Human readable name for the configuration
	 */
	@JsonProperty
	public Map<String, String> nameI18n;

	/**
	 * Human readable description for the configuration
	 */
	@JsonProperty
	public Map<String, String> descriptionI18n;

	/**
	 * The name of the mesh plugin which is to handle this form type
	 */
	@JsonProperty
	public String pluginName;

	/**
	 * Whether the form type configuration is for external forms
	 */
	@JsonProperty
	public boolean external;

	/**
	 * If the form should only be able to be submitted once per user
	 */
	@JsonProperty
	public boolean submitOnce;

	/**
	 * All available controls, where the key is the ID of the control
	 */
	@JsonProperty
	public Controls controls;

	/**
	 * All available blocks, where the key is the ID of the block
	 */
	@JsonProperty
	public Blocks blocks;

	/**
	 * Options which can be selected for user reference information
	 */
	@JsonProperty
	public List<SelectOption> userOptions;

	/**
	 * Which styling variations the form-type has
	 */
	@JsonProperty
	public List<SelectOption> templateOptions;

	/**
	 * Options which determine which email templates are available, which will be sent to the user upon submitting a form
	 */
	@JsonProperty
	public List<SelectOption> userEmailTemplateOptions;

	/**
	 * Options which determine which email templates are available, which will be sent to the admin emails, when a end-user submits a form
	 */
	@JsonProperty
	public List<SelectOption> adminEmailTemplateOptions;

	/**
	 * Definition of all available FormFlows for this formtypeconfig
	 */
	@JsonProperty
	public FormFlows flows;
}
