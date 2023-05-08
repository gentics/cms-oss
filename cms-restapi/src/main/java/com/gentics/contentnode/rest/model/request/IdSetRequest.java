package com.gentics.contentnode.rest.model.request;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request containing a list of IDs
 */
@XmlRootElement
public class IdSetRequest {
	/**
	 * Set of IDs
	 */
	private List<String> ids;

	/**
	 * Create an empty instance
	 */
	public IdSetRequest() {
	}

	/**
	 * Create an instance with a single ID
	 * @param id ID
	 */
	public IdSetRequest(String id) {
		setIds(Arrays.asList(id));
	}

	/**
	 * List of IDs contained in the request
	 * @return list of IDs
	 */
	public List<String> getIds() {
		return ids;
	}

	/**
	 * Set the ID list
	 * @param ids ID list
	 */
	public void setIds(List<String> ids) {
		this.ids = ids;
	}
}
