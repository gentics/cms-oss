package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Class representing a daisyDiff request sent to DiffResource. Encapsulates request data.
 */
@XmlRootElement
public class DaisyDiffRequest {
	
	/**
	 * Older version of a HTML document to be diff'ed
	 */
	protected String older;

	/**
	 * Newer version of a HTML document to be diff'ed
	 */
	protected String newer;

	/**
	 * Regex of content to be ignored while diffing
	 */
	protected String ignoreRegex;

	/**
	 * Create an empty instance (Used by JAXB)
	 */
	public DaisyDiffRequest() {}

	/**
	 * Get the older version of the html document to be diff'ed.
	 * 
	 * @return the older version of the HTML document to diff 
	 */
	public String getOlder() {
		return older;
	}

	/**
	 * Set the older version of the html document to be diff'ed.
	 * 
	 * @param older the older version of the HTML document to diff
	 */
	public void setOlder(String older) {
		this.older = older;
	}

	/**
	 * Get the newer version of the html document to be diff'ed.
	 * 
	 * @return the newer version of the HTML document to diff
	 */
	public String getNewer() {
		return newer;
	}

	/**
	 * Set the newer version of the html document to be diff'ed.
	 * 
	 * @param newer the newer version of the HTML document to diff 
	 */
	public void setNewer(String newer) {
		this.newer = newer;
	}

	/**
	 * Return the ignore regex that will be used to sanitize the content before the diff is invoked.
	 * 
	 * @return the the Regex of content to be ignored while diffing. May be null.
	 */
	public String getIgnoreRegex() {
		return ignoreRegex;
	}

	/**
	 * Return the ignore regex that will be used to sanitize the content before the diff is invoked.
	 * 
	 * @param ignoreRegex the Regex of content to be ignored while diffing. May be null.
	 */
	public void setIgnoreRegex(String ignoreRegex) {
		this.ignoreRegex = ignoreRegex;
	}
}
