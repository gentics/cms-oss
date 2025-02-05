package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response for the request to fetch user data
 */
@XmlRootElement
public class UserDataResponse extends GenericResponse {
	private JsonNode data;

	/**
	 * User data in JSON format
	 * @return user data
	 */
	public JsonNode getData() {
		return data;
	}

	/**
	 * Set the user data
	 * @param data user data
	 */
	public void setData(JsonNode data) {
		this.data = data;
	}
}
