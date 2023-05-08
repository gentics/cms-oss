package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Rest Model for object property categories
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectPropertyCategory implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3923056599089184376L;

	/**
	 * Id of the category
	 */
	private Integer id;

	/**
	 * Global ID
	 */
	private String globalId;

	/**
	 * Category name
	 */
	private String name;

	/**
	 * A map of language-name pairs
	 */
	private Map<String, String> nameI18n;

	/**
	 * Internal ID of the object property category
	 * @return ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the ID
	 * @param id ID
	 * @return fluent API
	 */
	public ObjectPropertyCategory setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Global ID
	 * @return global ID
	 */
	public String getGlobalId() {
		return globalId;
	}

	/**
	 * Set the global ID
	 * @param globalId global ID
	 * @return fluent API
	 */
	public ObjectPropertyCategory setGlobalId(String globalId) {
		this.globalId = globalId;
		return this;
	}

	/**
	 * Name in the current language
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 * @return fluent API
	 */
	public ObjectPropertyCategory setName(String name) {
		this.name = name;
		return this;
	}

	public Map<String, String> getNameI18n() {
		return nameI18n;
	}

	public ObjectPropertyCategory setNameI18n(Map<String, String> i18nName) {
		this.nameI18n = i18nName;
		return this;
	}

	public ObjectPropertyCategory setName(String name, String language) {
		if (this.nameI18n == null) {
			this.nameI18n = new HashMap<>();
		}
		this.nameI18n.put(language, name);
		return this;
	}
}
