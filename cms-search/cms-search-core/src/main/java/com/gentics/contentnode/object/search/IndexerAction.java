package com.gentics.contentnode.object.search;

/**
 * Possible values for DB column indexerqueue.action (which is also an ENUM, so this must be in sync)
 */
public enum IndexerAction {
	/**
	 * Put the object into the index
	 */
	put,

	/**
	 * Delete the object from the index
	 */
	delete
}
