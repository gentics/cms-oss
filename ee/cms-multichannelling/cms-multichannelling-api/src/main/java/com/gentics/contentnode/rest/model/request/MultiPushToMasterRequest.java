package com.gentics.contentnode.rest.model.request;

import java.util.List;
import java.util.Vector;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to push multiple objects (of same type) to the master
 */
@XmlRootElement
public class MultiPushToMasterRequest extends PushToMasterRequest {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1411288210639075174L;

	/**
	 * List of object ids
	 */
	private List<Integer> ids = new Vector<Integer>();

	/**
	 * Create an empty object
	 */
	public MultiPushToMasterRequest() {}

	/**
	 * Get the list of ids
	 * @return list of ids
	 */
	public List<Integer> getIds() {
		return ids;
	}

	/**
	 * Set the list of ids
	 * @param ids list of ids
	 */
	public void setIds(List<Integer> ids) {
		this.ids = ids;
	}
}
