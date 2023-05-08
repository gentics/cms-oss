package com.gentics.contentnode.rest.model.request;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request for publishing many pages
 */
@XmlRootElement
public class MultiPagePublishRequest extends PagePublishRequest {

	/**
	 * List of page ids
	 */
	protected List<String> ids;

	/**
	 * Wheter to use the publish at dates set in a page (true) or do an immediate publish (false, default)
	 */
	protected boolean keepPublishAt;
	
	/**
	 * Number of seconds the job may run in foreground
	 */
	private Integer foregroundTime;

	/**
	 * Get the page ids
	 * @return page ids
	 */
	public List<String> getIds() {
		return ids;
	}

	/**
	 * Set the page ids
	 * @param ids
	 */
	public void setIds(List<String> ids) {
		this.ids = ids;
	}

	public Integer getForegroundTime() {
		return foregroundTime;
	}

	public void setForegroundTime(Integer foregroundTime) {
		this.foregroundTime = foregroundTime;
	}

	public boolean isKeepPublishAt() {
		return keepPublishAt;
	}

	public void setKeepPublishAt(boolean keepPublishAt) {
		this.keepPublishAt = keepPublishAt;
	}
}
