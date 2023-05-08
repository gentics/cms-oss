package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request for taking a page offline
 */
@XmlRootElement
public class PageOfflineRequest {
	/**
	 * True if all languages variants of the page shall be taken offline, false to only take the given language variant offline
	 */
	private boolean alllang = false;

	/**
	 * Timestamp for taking the page(s) offline at a specific time. If set to 0, the page will be taken offline immediately
	 */
	private int at = 0;

	/**
	 * True if all languages shall be taken offline, false if not
	 * @return true or false
	 */
	public boolean isAlllang() {
		return alllang;
	}

	/**
	 * Timestamp to take the page offline at a specific time, 0 for taking the page offline immediately
	 * @return timestamp or 0
	 */
	public int getAt() {
		return at;
	}

	/**
	 * Set whether all languages shall be taken offline
	 * @param alllang true if all language variants shall be taken offline, false if not
	 */
	public void setAlllang(boolean alllang) {
		this.alllang = alllang;
	}

	/**
	 * Set timestamp to take the page offline at a specific timestamp, 0 for taking the page offline immediately
	 * @param at timestamp or 0
	 */
	public void setAt(int at) {
		this.at = at;
	}
}
