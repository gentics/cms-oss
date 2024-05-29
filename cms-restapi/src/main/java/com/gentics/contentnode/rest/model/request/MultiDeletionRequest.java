package com.gentics.contentnode.rest.model.request;

import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Multi object deletion request
 */
@XmlRootElement
public class MultiDeletionRequest {
	/**
	 * Object IDs to delete
	 */
	protected Set<String> ids;

	/**
	 * Number of seconds the job may run in foreground
	 */
	private Integer foregroundTime;

	/**
	 * JAXB ctor
	 */
	public MultiDeletionRequest() {}

	public Set<String> getIds() {
		return ids;
	}

	public void setIds(Set<String> ids) {
		this.ids = ids;
	}

	public Integer getForegroundTime() {
		return foregroundTime;
	}

	public void setForegroundTime(Integer foregroundTime) {
		this.foregroundTime = foregroundTime;
	}
}
