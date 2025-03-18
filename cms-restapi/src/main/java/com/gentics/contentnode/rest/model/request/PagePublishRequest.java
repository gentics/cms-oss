/*
 * @author norbert
 * @date 14.03.2011
 * @version $Id: PagePublishRequest.java,v 1.1.2.1 2011-03-14 15:12:26 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request for publishing a page
 */
@XmlRootElement
public class PagePublishRequest {

	/**
	 * Message accompanying the publish request
	 */
	private String message;

	/**
	 * True if all languages variants of the page shall be published, false to only publish the given language variant
	 */
	private boolean alllang = false;

	/**
	 * Timestamp for publishing the page(s) at a specific time. If set to 0, the page will be published immediately
	 */
	private int at = 0;

	/**
	 * True to keep the current "publishAt" version (if one set)
	 */
	private boolean keepVersion = false;

	/**
	 * Empty constructor
	 */
	public PagePublishRequest() {}

	/**
	 * Get the message
	 * @return message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Get true if all languages shall be published, false if not
	 * @return true or false
	 */
	public boolean isAlllang() {
		return alllang;
	}

	/**
	 * Get the timestamp to publish the page at a specific time, 0 for publishing the page immediately
	 * @return timestamp or 0
	 */
	public int getAt() {
		return at;
	}

	/**
	 * Set the message
	 * @param message message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Set whether all languages shall be published
	 * @param alllang true if all language variants shall be published, false if not
	 */
	public void setAlllang(boolean alllang) {
		this.alllang = alllang;
	}

	/**
	 * Set timestamp to publish the page at a specific timestamp, 0 for publishing the page immediately
	 * @param at timestamp or -1
	 */
	public void setAt(int at) {
		this.at = at;
	}

	/**
	 * True if the currently set "publishAt" version shall be kept (and only the publishAt time shall be changed).
	 * If there is no publishAt set, or the page is published immediately, this parameter is ignored.
	 * @return flag to keep version
	 */
	public boolean isKeepVersion() {
		return keepVersion;
	}

	/**
	 * Set "keepVersion" flag
	 * @param keepVersion flag
	 */
	public void setKeepVersion(boolean keepVersion) {
		this.keepVersion = keepVersion;
	}
}
