package com.gentics.contentnode.rest.model.linkchecker;

/**
 * Scope of link replacement
 */
public enum ReplaceScope {
	/**
	 * Replace the link only
	 */
	link,

	/**
	 * Replace all occurrences in the page
	 */
	page,

	/**
	 * Replace all occurrences in the node
	 */
	node,

	/**
	 * Replace all occurrences in the system
	 */
	global;
}
