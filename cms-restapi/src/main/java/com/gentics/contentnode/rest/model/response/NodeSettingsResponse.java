package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response to a request for loading node specific settings.
 */
@XmlRootElement
public class NodeSettingsResponse extends GenericResponse {

	/** Node specific settings */
	private JsonNode data;
	/**
	 * Create an empty instance.
	 */
	public NodeSettingsResponse() {
		data = null;
	}

	/**
	 * Create an instance with empty settings and given message and response information.
	 *
	 * @param responseInfo The response information
	 */
	public NodeSettingsResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
		data = null;
	}

	/**
	 * Create an instance with the given settings and response information, but without message.
	 *
	 * @param data The node settings
	 * @param responseInfo The response information
	 */
	public NodeSettingsResponse(JsonNode data, ResponseInfo responseInfo) {
		super(null, responseInfo);
		this.data = data;
	}

	/**
	 * Get the loaded node settings.
	 *
	 * @return The loaded node settings
	 */
	public JsonNode getData() {
		return data;
	}

	/**
	 * Set the loaded node settings.
	 *
	 * @param data The loaded node settings
	 */
	public void setData(JsonNode data) {
		this.data = data;
	}
}
