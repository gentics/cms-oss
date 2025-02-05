package com.gentics.contentnode.rest.model.request;

import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to create tags
 */
@XmlRootElement
public class MultiTagCreateRequest {
	/**
	 * Mapping of ID to TagCreateRequest object
	 */
	private Map<String, TagCreateRequest> create;

	/**
	 * Create empty instance
	 */
	public MultiTagCreateRequest() {}

	/**
	 * Map of temporary IDs to tags to create
	 * @return map of IDs to create info
	 */
	public Map<String, TagCreateRequest> getCreate() {
		return create;
	}

	/**
	 * Set the map of IDs to create info
	 * @param create map of IDs to create info
	 */
	public void setCreate(Map<String, TagCreateRequest> create) {
		this.create = create;
	}
}
