package com.gentics.contentnode.rest.model.request;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request object for moving multiple objects
 */
@XmlRootElement
public class MultiObjectMoveRequest extends ObjectMoveRequest {
	/**
	 * List of object IDs
	 */
	private List<String> ids;

	/**
	 * Create an empty instance
	 */
	public MultiObjectMoveRequest() {
	}

	/**
	 * Get the list of ids
	 * @return list of ids
	 */
	public List<String> getIds() {
		return ids;
	}

	/**
	 * Set the list of ids
	 * @param ids list of ids
	 */
	public void setIds(List<String> ids) {
		this.ids = ids;
	}
}
