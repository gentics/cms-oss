package com.gentics.contentnode.rest.util;

import java.util.Collection;
import java.util.Iterator;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Abstract filter for filtering NodeObject that contains the default implementation for filtering collections of objects.
 * All Implementations of Filters should extend this class
 */
public abstract class AbstractNodeObjectFilter implements NodeObjectFilter {

	@Override
	public void filter(Collection<? extends NodeObject> objects) throws NodeException {
		// remove all objects from the given collection, that do not match the filter
		for (Iterator<? extends NodeObject> i = objects.iterator(); i.hasNext();) {
			NodeObject nodeObject = i.next();

			if (!matches(nodeObject)) {
				i.remove();
			}
		}
	}
}
