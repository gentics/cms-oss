package com.gentics.contentnode.rest.model.request;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MultiPageAssignRequest {

	/**
	 * Message accompanying the assign request.
	 */
	private String message;

	/**
	 * List of page ids
	 */
	private List<String> pageIds;

	/**
	 * List of user ids
	 */
	private List<Integer> userIds;

	/**
	 * Return the message which will be send to the listed users.
	 * 
	 * @return
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the message which will be send to listed users.
	 * 
	 * @param message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Return the list of page ids.
	 * 
	 * @return
	 */
	public List<String> getPageIds() {
		return pageIds;
	}

	/**
	 * Set the list of page ids.
	 * 
	 * @param pageIds
	 */
	public void setPageIds(List<String> pageIds) {
		this.pageIds = pageIds;
	}

	/**
	 * Get the list of user ids for users which should be informed about the
	 * assign operation.
	 * 
	 * @return
	 */
	public List<Integer> getUserIds() {
		return userIds;
	}

	/**
	 * Set the list of user ids which should be informed about the assign
	 * operation.
	 * 
	 * @param userIds
	 */
	public void setUserIds(List<Integer> userIds) {
		this.userIds = userIds;
	}

}
