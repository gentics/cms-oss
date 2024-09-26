package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Time Management of forms
 */
@XmlRootElement
public class FormTimeManagement implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3172410665795732575L;

	/**
	 * Time at which the form will be published
	 */
	private Integer at;

	/**
	 * Version that will be published
	 */
	private ItemVersion version;

	/**
	 * Time at which the form will be taken offline
	 */
	private Integer offlineAt;

	/**
	 * the future publisher (i.e.: the user that planned to publish something)
	 */
	public User futurePublisher;

	/**
	 * the future unpublisher (i.e.: the user that planned to unpublish something)
	 */
	public User futureUnpublisher;


	/**
	 * Get the timestamp at which the form will be published
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
	public FormTimeManagement setAt(Integer at) {
		this.at = at;
		return this;
	}

	/**
	 * Form Version that will be published at the timestamp
	 * @return form version
	 */
	public ItemVersion getVersion() {
		return version;
	}

	/**
	 * Set the version to be published
	 * @param version form version
	 * @return fluent API
	 */
	public FormTimeManagement setVersion(ItemVersion version) {
		this.version = version;
		return this;
	}

	/**
	 * Time at which the form will be taken offline
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
	public FormTimeManagement setOfflineAt(Integer offlineAt) {
		this.offlineAt = offlineAt;
		return this;
	}

	/**
	 * Gets user that planned to publish something
	 * @return the future publisher
	 */
	public User getFuturePublisher() {
		return futurePublisher;
	}

	/**
	 * Sets the future publisher (i.e.: the user that planned to publish something)
	 * @param futurePublisher
	 * @return fluent API
	 */
	public FormTimeManagement setFuturePublisher(User futurePublisher) {
		this.futurePublisher = futurePublisher;
		return this;
	}

	/**
	 * Gets user that planned to upublish something
	 * @return the future upublisher
	 */
	public User getFutureUnpublisher() {
		return futureUnpublisher;
	}

	/**
	 * Sets the future unpublisher (i.e.: the user that planned to unpublish something)
	 * @param futureUnpublisher
	 * @return fluent API
	 */
	public FormTimeManagement setFutureUnpublisher(User futureUnpublisher) {
		this.futureUnpublisher = futureUnpublisher;
		return this;
	}
}
