package com.gentics.contentnode.rest.model.staging;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Base model for content package
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ContentPackageBase implements Serializable {

	private static final long serialVersionUID = 8465031234023365161L;

	private String name;
	private String description;
	private Integer timestamp;

	/**
	 * Name of the content package
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Content package description
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description
	 * @param description description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Timestamp of the last modification
	 * @return modification timestamp
	 */
	public Integer getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the modification timestamp
	 * @param timestamp timestamp
	 */
	public void setTimestamp(Integer timestamp) {
		this.timestamp = timestamp;
	}

	public void clone(ContentPackageBase origin, boolean cloneTimestamp) {
		if (origin.name != null && !origin.name.trim().equals("")) {
			this.name = origin.name;
		}
		if (origin.description != null && !origin.description.trim().equals("")) {
			this.description = origin.description;
		}
		if (cloneTimestamp && origin.timestamp != null) {
			this.timestamp = origin.timestamp;
		}
	}
}
