package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response containing the Usersnap settings
 */
@XmlRootElement
public class UsersnapResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private JsonNode settings;

	/**
	 * Create an instance with response code "ok"
	 */
	public UsersnapResponse() {
		super(null, ResponseInfo.ok(""));
	}

	/**
	 * Usersnap settings
	 * @return settings
	 */
	public JsonNode getSettings() {
		return settings;
	}

	/**
	 * Set Usersnap settings
	 * @param settings settings
	 * @return fluent API
	 */
	public UsersnapResponse setSettings(JsonNode settings) {
		this.settings = settings;
		return this;
	}
}
