package com.gentics.contentnode.rest.model.request;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request containing a list of page IDs and a flag for whether all language
 * variants of the specified pages shall be affected
 */
@XmlRootElement
public class PageIdSetRequest extends IdSetRequest {
	/**
	 * Flag for all language variants
	 */
	protected boolean alllangs = false;

	/**
	 * Create empty instance
	 */
	public PageIdSetRequest() {
	}

	/**
	 * Create an instance with a single ID
	 * @param id ID
	 */
	public PageIdSetRequest(String id) {
		setIds(Arrays.asList(id));
	}

	/**
	 * True if all language variants of the given pages shall be affected, false if not
	 * @return true for all language variants
	 */
	public boolean isAlllangs() {
		return alllangs;
	}

	/**
	 * Set true to handle all language variants
	 * @param alllangs true for all language variants
	 */
	public void setAlllangs(boolean alllangs) {
		this.alllangs = alllangs;
	}
}
