package com.gentics.contentnode.rest.util;

import java.util.Collection;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Interface for filters for filtering NodeObjects.
 */
public interface NodeObjectFilter {

	/**
	 * Check whether the given object matches the filter
	 * @param object object to check
	 * @return true when the object matches, false if not
	 * @throws NodeException
	 */
	boolean matches(NodeObject object) throws NodeException;

	/**
	 * Filter the given collection of NodeObjects with this filter implementation
	 * 
	 * @param objects
	 *            collection of NodeObjects to filter. non-matching objects will be
	 *            removed from the collection
	 * @throws NodeException
	 */
	void filter(Collection<? extends NodeObject> objects) throws NodeException;
}
