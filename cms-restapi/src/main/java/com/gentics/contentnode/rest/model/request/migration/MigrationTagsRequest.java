package com.gentics.contentnode.rest.model.request.migration;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request object for a request to retrieve tag types for tag migration
 * 
 * @author Taylor
 */
@XmlRootElement
public class MigrationTagsRequest {

	/**
	 * List of object ids to retrieve tag types for
	 */
	private List<Integer> ids;

	/**
	 * Object type
	 */
	private String type;

	/**
	 * Get the item type
	 * 
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the object type
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Get the object ids
	 * 
	 * @return object ids
	 */
	public List<Integer> getIds() {
		return ids;
	}

	/**
	 * Set the object ids
	 * 
	 * @param ids
	 */
	public void setIds(List<Integer> ids) {
		this.ids = ids;
	}

}
