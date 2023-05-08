package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * REST Model of a datasource
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Datasource implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8571394501375418454L;

	/**
	 * Id of the construct
	 */
	private Integer id;

	/**
	 * Global ID
	 */
	private String globalId;

	/**
	 * Type
	 */
	private DatasourceType type;

	/**
	 * Name
	 */
	private String name;

	/**
	 * Create empty instance
	 */
	public Datasource() {
	}

	/**
	 * Internal ID
	 * @return internal ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the internal ID
	 * @param id ID
	 * @return fluent API
	 */
	public Datasource setId(Integer id) {
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
	 * @param globalId
	 * @return fluent API
	 */
	public Datasource setGlobalId(String globalId) {
		this.globalId = globalId;
		return this;
	}

	/**
	 * Datasource type
	 * @return type
	 */
	public DatasourceType getType() {
		return type;
	}

	/**
	 * Set the type
	 * @param type type
	 * @return fluent API
	 */
	public Datasource setType(DatasourceType type) {
		this.type = type;
		return this;
	}

	/**
	 * Name
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
	public Datasource setName(String name) {
		this.name = name;
		return this;
	}
}
