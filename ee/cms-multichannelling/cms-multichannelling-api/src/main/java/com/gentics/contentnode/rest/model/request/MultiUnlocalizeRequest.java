package com.gentics.contentnode.rest.model.request;

import java.util.List;
import java.util.Vector;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to unlocalize multiple objects of same type
 */
@XmlRootElement
public class MultiUnlocalizeRequest extends UnlocalizeRequest {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8385166962327375086L;

	/**
	 * List of object ids
	 */
	private List<Integer> ids = new Vector<Integer>();

	/**
	 * Create an empty object
	 */
	public MultiUnlocalizeRequest() {}

	/**
	 * Create a wrapper for a single unlocalize request.
	 *
	 * @param id The ID of the object to unlocalize.
	 * @param request The request to wrap.
	 */
	public MultiUnlocalizeRequest(Integer id, UnlocalizeRequest request) {
		super(request);

		ids.add(id);
	}

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
