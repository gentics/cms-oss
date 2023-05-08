package com.gentics.contentnode.rest.util;

import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Filter for grouping subfilters. NodeObjects match this filter, when it
 * matches (at least) ONE of the subfilters. If no subfilter is defined, all
 * objects match
 */
public class OrFilter extends AbstractNodeObjectFilter {

	/**
	 * list of subfilters
	 */
	protected List<NodeObjectFilter> filters = new Vector<NodeObjectFilter>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gentics.contentnode.rest.util.NodeObjectFilter#matches(com.gentics
	 * .lib.base.object.NodeObject)
	 */
	public boolean matches(NodeObject object) throws NodeException {
		// if no subfilter is there, all objects match
		if (filters.isEmpty()) {
			return true;
		}

		// iterate through all subfilters
		for (NodeObjectFilter f : filters) {
			// when we found a matching filter, the object matches the OrFilter
			if (f.matches(object)) {
				return true;
			}
		}

		// we found no matching subfilter, so the object does not match
		return false;
	}

	/**
	 * Add a subfilter
	 * 
	 * @param filter
	 *            subfilter
	 */
	public void addFilter(NodeObjectFilter filter) {
		filters.add(filter);
	}
}
