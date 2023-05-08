package com.gentics.contentnode.rest.model.response.admin;

import java.util.List;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.webcohesion.enunciate.metadata.DocumentationExample;

/**
 * Response containing the available updates
 */
public class UpdatesInfoResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	protected List<String> available;

	/**
	 * List of available updates
	 * @return available updates
	 */
	@DocumentationExample(value = "5.34.23", value2 = "5.35.6")
	public List<String> getAvailable() {
		return available;
	}

	/**
	 * Set available updates
	 * @param available updates
	 */
	public void setAvailable(List<String> available) {
		this.available = available;
	}
}
