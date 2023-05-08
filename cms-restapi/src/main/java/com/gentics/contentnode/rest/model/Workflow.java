/*
 * @author norbert
 * @date 14.03.2011
 * @version $Id: Workflow.java,v 1.1.2.2 2011-03-18 10:24:14 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Workflow information about the page
 */
@XmlRootElement
public class Workflow implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8371507340534239162L;

	/**
	 * Get the groups to which the page is currently assigned
	 */
	private List<Group> groups;

	/**
	 * Get the last message in the workflow
	 */
	private String message;

	/**
	 * Get the user who put the page into the current workflow step
	 */
	private User user;

	/**
	 * True when the page has been modified while being in this workflow step
	 */
	private boolean modified;

	/**
	 * Timestamp, when the current step was initiated
	 */
	private int timestamp;

	/**
	 * Create an empty instance
	 */
	public Workflow() {}

	/**
	 * @return the groups
	 */
	public List<Group> getGroups() {
		return groups;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * @return the modified
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * @param groups the groups to set
	 */
	public void setGroups(List<Group> groups) {
		this.groups = groups;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * @param modified the modified to set
	 */
	public void setModified(boolean modified) {
		this.modified = modified;
	}

	/**
	 * @return the timestamp
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
}
