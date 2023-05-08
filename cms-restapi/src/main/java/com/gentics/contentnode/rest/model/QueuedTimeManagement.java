/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: TimeManagement.java,v 1.1.6.1 2011-03-09 17:42:43 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Queued TimeManagement
 */
@XmlRootElement
public class QueuedTimeManagement implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5255065062283053906L;

	/**
	 * Time at which the page shall be published/taken offline
	 */
	private Integer at;

	/**
	 * Version that will be published
	 */
	private PageVersion version;

	/**
	 * User who put the page into queue
	 */
	private User user;

	/**
	 * Constructor used by JAXB
	 */
	public QueuedTimeManagement() {}

	/**
	 * Get the timestamp at which the page shall be published/taken offline
	 * @return timestamp
	 */
	public Integer getAt() {
		return at;
	}

	/**
	 * Set the at timestamp
	 * @param at timestamp
	 * @return fluent API
	 */
	public QueuedTimeManagement setAt(Integer at) {
		this.at = at;
		return this;
	}

	/**
	 * Page Version that will be published at the timestamp
	 * @return page version
	 */
	public PageVersion getVersion() {
		return version;
	}

	/**
	 * Set the version to be published
	 * @param version page version
	 * @return fluent API
	 */
	public QueuedTimeManagement setVersion(PageVersion version) {
		this.version = version;
		return this;
	}

	/**
	 * User who put the page into the queue
	 * @return user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Set the user
	 * @param user user
	 * @return fluent API
	 */
	public QueuedTimeManagement setUser(User user) {
		this.user = user;
		return this;
	}
}
