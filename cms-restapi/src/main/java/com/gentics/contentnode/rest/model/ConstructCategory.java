package com.gentics.contentnode.rest.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Construct Category
 */
@XmlRootElement
public class ConstructCategory {

	Map<String, Construct> constructs = new LinkedHashMap<String, Construct>();

	/**
	 * Id of the category
	 */
	private Integer id;

	/**
	 * Global ID
	 */

	private String globalId;

	/**
	 * Simple name
	 */
	private String name;

	/**
	 * Language-name pairs
	 */
	private Map<String, String> nameI18n;

	/**
	 * Sort order.
	 */
	private Integer sortOrder;

	public ConstructCategory() {}

	/**
	 * Creates a construct category with the given name
	 * @param name
	 */
	public ConstructCategory(String name) {
		this.name = name;
	}

	/**
	 * Name of this construct category
	 *
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of this construct category
	 *
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Adds the construct with the given name to the map of constructs of this category
	 *
	 * @param name
	 * @param construct
	 */
	public void addConstruct(String name, Construct construct) {
		constructs.put(name, construct);
	}

	/**
	 * Map of constructs for this category
	 *
	 * @return
	 */
	public Map<String, Construct> getConstructs() {
		return this.constructs;
	}

	/**
	 * Sets the map with constructs for this category
	 *
	 * @param constructs
	 */
	public void setConstructs(Map<String, Construct> constructs) {
		this.constructs = constructs;
	}

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
	public ConstructCategory setId(Integer id) {
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
	public ConstructCategory setGlobalId(String globalId) {
		this.globalId = globalId;
		return this;
	}

	public Map<String, String> getNameI18n() {
		return nameI18n;
	}

	public ConstructCategory setNameI18n(Map<String, String> i18nName) {
		this.nameI18n = i18nName;
		return this;
	}

	public ConstructCategory setName(String name, String language) {
		if (this.nameI18n == null) {
			this.nameI18n = new HashMap<>();
		}
		this.nameI18n.put(language, name);
		return this;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public ConstructCategory setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;

		return this;
	}
}
