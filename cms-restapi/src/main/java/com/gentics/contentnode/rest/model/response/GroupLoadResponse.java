package com.gentics.contentnode.rest.model.response;

import com.gentics.contentnode.rest.model.Group;

/**
 * Response containing a single group
 */
public class GroupLoadResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5470364881815549450L;

	/**
	 * The loaded group
	 */
	private Group group;

	/**
	 * Create an empty instance
	 */
	public GroupLoadResponse() {}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public GroupLoadResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with message, response info and group
	 * @param message message
	 * @param responseInfo response info
	 * @param group group
	 */
	public GroupLoadResponse(Message message, ResponseInfo responseInfo, Group group) {
		super(message, responseInfo);
		setGroup(group);
	}

	/**
	 * Set the group
	 * @param group group
	 */
	public void setGroup(Group group) {
		this.group = group;
	}

	/**
	 * Get the group
	 * @return group
	 */
	public Group getGroup() {
		return group;
	}
}
