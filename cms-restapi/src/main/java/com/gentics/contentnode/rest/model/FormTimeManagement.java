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
}
