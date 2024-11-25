package com.gentics.contentnode.object;

import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.ContentNodeDate;

/**
 * Node object with creation and edit timestamp metadata.
 */
public interface MetaDateNodeObject extends NodeObject, Resolvable {

	/**
	 * get the creation date as a unix timestamp 
	 * @return creation date unix timestamp
	 */
	ContentNodeDate getCDate();

	/**
	 * get the edit date as a unix timestamp 
	 * @return edit date unix timestamp
	 */
	ContentNodeDate getEDate();
}
