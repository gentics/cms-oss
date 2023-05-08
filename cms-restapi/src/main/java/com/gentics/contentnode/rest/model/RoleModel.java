package com.gentics.contentnode.rest.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model for a role
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleModel {
	private Integer id;

	/**
	 * Name of the role
	 */
	private String name;

	/**
	 * Description of the role
	 */
	private String description;

	/**
	 * Translated names
	 */
	private Map<String, String> nameI18n;

	/**
	 * Translated description
	 */
	private Map<String, String> descriptionI18n;

	/**
	 * Role ID
	 * @return id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the ID
	 * @param id ID
	 * @return fluent API
	 */
	public RoleModel setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Get name in the current language
	 * @return name
	 */
	public String getName() {
		return name;
	}


	/**
	 * Set name in the current language
	 * @return fluent API
	 */
	public RoleModel setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Get the description in the current language
	 * @return name
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description in the current language
	 * @return fluent API
	 */
	public RoleModel setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Name in all possible translations
	 * @return name map
	 */
	public Map<String, String> getNameI18n() {
		return nameI18n;
	}

	/**
	 * Set name map
	 * @param nameI18n name map
	 * @return fluent API
	 */
	public RoleModel setNameI18n(Map<String, String> nameI18n) {
		this.nameI18n = nameI18n;
		return this;
	}

	/**
	 * Description in all possible translations
	 * @return description map
	 */
	public Map<String, String> getDescriptionI18n() {
		return descriptionI18n;
	}

	/**
	 * Set description map
	 * @param descriptionI18n description map
	 * @return fluent API
	 */
	public RoleModel setDescriptionI18n(Map<String, String> descriptionI18n) {
		this.descriptionI18n = descriptionI18n;
		return this;
	}
}
