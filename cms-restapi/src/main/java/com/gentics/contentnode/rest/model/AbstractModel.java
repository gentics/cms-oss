package com.gentics.contentnode.rest.model;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.webcohesion.enunciate.metadata.DocumentationExample;

/**
 * Abstract base class for REST Model implementations that have a global Id
 */
@XmlRootElement
public abstract class AbstractModel {
	/**
	 * Global ID
	 */
	private String globalId;

	/**
	 * Global ID
	 * @return global ID
	 */
	@DocumentationExample(value = "0737.82ae915c-059c-11f0-ae44-482ae36fb1c5", value2 = "0737.34f95a21-0304-11f0-ae44-482ae36fb1c5")
	public String getGlobalId() {
		return globalId;
	}

	/**
	 * Set the global ID
	 * @param globalId global ID
	 */
	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}
}
