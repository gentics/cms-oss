package com.gentics.contentnode.rest.model;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PartType {

	private int id;
	private String name;
	private String description;
	private String javaClass;
	private boolean deprecated;

	/**
	 * The name of the part type
	 * 
	 * @return name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Set the name of the part type
	 * 
	 * @return fluent API
	 */
	public PartType setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Get the id of the part type
	 * 
	 * @return the part type id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the id of the part type
	 * 
	 * @return fluent API
	 */
	public PartType setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Get the description of the part type
	 * 
	 * @return the description of the part type
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description of the part type
	 * 
	 * @return fluent API
	 */
	public PartType setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Get the java class (implementation) of the part type
	 * 
	 * @return the implementation class
	 */
	public String getJavaClass() {
		return javaClass;
	}

	/**
	 * Set the java class (implementation) of the part type
	 * 
	 * @return fluent API
	 */
	public PartType setJavaClass(String javaClass) {
		this.javaClass = javaClass;
		return this;
	}

	/**
	 * The deprecation flag
	 * 
	 * @return ture if the part type is deprecated
	 */
	public boolean isDeprecated() {
		return deprecated;
	}

	/**
	 * Set the java class (implementation) of the part type
	 * 
	 * @return fluent API
	 */
	public PartType setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
		return this;
	}

}