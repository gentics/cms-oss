package com.gentics.lib.content;

import com.gentics.lib.base.CMSUnavailableException;

/**
 * @author l.herlt@gentics.com
 */
public interface GenticsContentResult {

	static final int STATUS_OK = 1;

	static final int STATUS_NOTFOUND = 2;

	// /////////////////////////////////////
	// operations

	/**
	 * Get the status of this result object.
	 * @return one of GenticsContentResult.STATUS_*
	 */
	int getStatus();

	/**
	 * @return the amount of results that you can fetch via getNextObject()
	 */
	int size();

	/**
	 * Get the next GenticsContentObject within this result. null if there are
	 * no (more) objects.
	 * @return the next GenticsContentObject or null
	 */
	GenticsContentObject getNextObject() throws CMSUnavailableException;

} // end GenticsContentResult

