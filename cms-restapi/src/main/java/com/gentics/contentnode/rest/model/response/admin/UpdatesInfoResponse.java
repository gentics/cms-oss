package com.gentics.contentnode.rest.model.response.admin;

import java.util.List;

import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Response containing the available updates
 */
public class UpdatesInfoResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	protected List<Update> available;

	/**
	 * List of available updates
	 * @return available updates
	 */
	public List<Update> getAvailable() {
		return available;
	}

	/**
	 * Set available updates
	 * @param available updates
	 */
	public void setAvailable(List<Update> available) {
		this.available = available;
	}
}
