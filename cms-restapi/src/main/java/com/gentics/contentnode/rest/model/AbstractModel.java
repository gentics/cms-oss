package com.gentics.contentnode.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

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
