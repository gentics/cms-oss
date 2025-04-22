package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import com.webcohesion.enunciate.metadata.DocumentationExample;

/**
 * Model for breadcrumbs items
 */
public class BreadcrumbItem implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6166340176671857467L;

	/**
	 * Id of the item
	 */
	private Integer id;

	/**
	 * global ID of the item
	 */
	private String globalId;

	/**
	 * Name of the item
	 */
	private String name;

	/**
	 * ID of the folder
	 * @return ID
	 */
	@DocumentationExample(value = "28", value2 = "799")
	public Integer getId() {
		return id;
	}

	/**
	 * Set the ID
	 * @param id ID
	 * @return fluent API
	 */
	public BreadcrumbItem setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Global ID
	 * @return global ID
	 */
	@DocumentationExample(value = "0737.1de1eecb-059c-11f0-ae44-482ae36fb1c5", value2 = "0737.c2b67126-0303-11f0-ae44-482ae36fb1c5")
	public String getGlobalId() {
		return globalId;
	}

	/**
	 * Set the global ID
	 * @param globalId global ID
	 * @return fluent API
	 */
	public BreadcrumbItem setGlobalId(String globalId) {
		this.globalId = globalId;
		return this;
	}

	/**
	 * Name of the folder
	 * @return name
	 */
	@DocumentationExample(value = "Home", value2 = "News")
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 * @return fluent API
	 */
	public BreadcrumbItem setName(String name) {
		this.name = name;
		return this;
	}
}
