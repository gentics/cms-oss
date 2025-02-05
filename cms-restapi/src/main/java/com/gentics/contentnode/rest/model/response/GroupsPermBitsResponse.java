package com.gentics.contentnode.rest.model.response;

import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing permission and role bits
 */
@XmlRootElement
public class GroupsPermBitsResponse extends GenericResponse {
	/**
	 * Map of groups
	 * <groupId, Permission bits string>
	 */
	private Map<Integer, String> groups;

	/**
	 * Create an empty instance
	 */
	public GroupsPermBitsResponse() {}

	/**
	 * Creates a response with a group permission bit map
	 *
	 * @param message       Message object
	 * @param responseInfo  ResponseInfo object
	 */
	public GroupsPermBitsResponse(Message message, ResponseInfo responseInfo, Map<Integer, String> groups) {
		super(message, responseInfo);
		this.groups = groups;
	}

	/**
	 * Get the groups
	 *
	 * @return The groups
	 */
	public Map<Integer, String> getGroups() {
		return groups;
	}

	/**
	 * Set the groups
	 * @param groups groups
	 */
	public void setGroups(Map<Integer, String> groups) {
		this.groups = groups;
	}
}
