package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Class representing the response to a diff request sent to {@link DiffResource}.
 */
@XmlRootElement
public class DiffResponse {

	/**
	 * The diff
	 */
	protected String diff;

	/**
	 * Create an empty instance (Used by JAXB)
	 */
	public DiffResponse() {}

	/**
	 * @return the diff
	 */
	public String getDiff() {
		return diff;
	}

	/**
	 * @param diff the diff to set
	 */
	public void setDiff(String diff) {
		this.diff = diff;
	}
}
