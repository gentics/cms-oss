/*
 * @author norbert
 * @date 16.03.2011
 * @version $Id: WorkflowRequest.java,v 1.1.2.2 2011-03-17 14:26:40 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request for modifying a page in the workflow
 */
@XmlRootElement
public class WorkflowRequest {

	/**
	 * Message in the workflow request
	 */
	private String message;

	/**
	 * ID of the group to which the page shall be assigned in the workflow
	 */
	private Integer group;

	/**
	 * True when the workflow shall be deleted
	 */
	private boolean delete;

	/**
	 * Empty constructor
	 */
	public WorkflowRequest() {}

	/**
	 * Get the message
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Get the group ID
	 * @return the group
	 */
	public Integer getGroup() {
		return group;
	}

	/**
	 * Set the message
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Set the group ID
	 * @param group the group to set
	 */
	public void setGroup(Integer group) {
		this.group = group;
	}

	/**
	 * @return the delete
	 */
	public boolean isDelete() {
		return delete;
	}

	/**
	 * @param delete the delete to set
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}
}
