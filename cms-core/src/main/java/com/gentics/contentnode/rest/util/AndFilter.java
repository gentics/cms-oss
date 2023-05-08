package com.gentics.contentnode.rest.util;

import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Filter for grouping subfilters. NodeObjects match this filter, when it
 * matches ALL subfilters. If no subfilter is defined, all objects match
 */
public class AndFilter extends AbstractNodeObjectFilter {

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
		// iterate through all filters and check whether they all match
		for (NodeObjectFilter f : filters) {
			// if one filter does not match, the AndFilter does not match
			if (!f.matches(object)) {
				return false;
			}
		}

		// all filters matched (or there were not subfilters), so the AndFilter
		// matches
		return true;
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
