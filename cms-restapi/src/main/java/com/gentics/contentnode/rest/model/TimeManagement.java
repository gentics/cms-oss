package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Time Management of pages
 */
@XmlRootElement
public class TimeManagement implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2957689883662857966L;

	/**
	 * Time at which the page will be published
	 */
	private Integer at;

	/**
	 * Version that will be published
	 */
	private PageVersion version;

	/**
	 * Time at which the page will be taken offline
	 */
	private Integer offlineAt;

	/**
	 * Queued publish timemanagement
	 */
	private QueuedTimeManagement queuedPublish;

	/**
	 * Queued offline timemanagement
	 */
	private QueuedTimeManagement queuedOffline;

	/**
	 * Get the timestamp at which the page will be published
	 * @return publish timestamp
	 */
	public Integer getAt() {
		return at;
	}

	/**
	 * Set the at timestamp
	 * @param at timestamp
	 * @return fluent API
	 */
	public TimeManagement setAt(Integer at) {
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
	public TimeManagement setVersion(PageVersion version) {
		this.version = version;
		return this;
	}

	/**
	 * Time at which the page will be taken offline
	 * @return timestamp to take offline
	 */
	public Integer getOfflineAt() {
		return offlineAt;
	}

	/**
	 * Set timestamp for taking offline
	 * @param offlineAt timestamp
	 * @return fluent API
	 */
	public TimeManagement setOfflineAt(Integer offlineAt) {
		this.offlineAt = offlineAt;
		return this;
	}

	/**
	 * Queued time management for publishing the page
	 * @return queued time management
	 */
	public QueuedTimeManagement getQueuedPublish() {
		return queuedPublish;
	}

	/**
	 * Set queued time management
	 * @param queuedPublish queued time management
	 * @return fluent API
	 */
	public TimeManagement setQueuedPublish(QueuedTimeManagement queuedPublish) {
		this.queuedPublish = queuedPublish;
		return this;
	}

	/**
	 * Queued time management for taking the page offline
	 * @return queued time management
	 */
	public QueuedTimeManagement getQueuedOffline() {
		return queuedOffline;
	}

	/**
	 * Set queued time management
	 * @param queuedOffline queued time management
	 * @return fluent API
	 */
	public TimeManagement setQueuedOffline(QueuedTimeManagement queuedOffline) {
		this.queuedOffline = queuedOffline;
		return this;
	}
}
