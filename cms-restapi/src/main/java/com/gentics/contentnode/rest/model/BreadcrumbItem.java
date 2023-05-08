package com.gentics.contentnode.rest.model;

import java.io.Serializable;

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
