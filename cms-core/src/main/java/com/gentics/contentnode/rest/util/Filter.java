package com.gentics.contentnode.rest.util;

import java.util.Iterator;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;

/**
 * Interface for a generic filter
 *
 * @param <T> clazz of filtered objects
 */
public interface Filter<T> {
	/**
	 * Check whether the given object matches the filter
	 * 
	 * @param object
	 *            object to check
	 * @return true when the object matches, false if not
	 * @throws NodeException
	 */
	boolean matches(T object) throws NodeException;

	/**
	 * Filter the given list of Objects with this filter implementation
	 *
	 * @param objects
	 *            list of objects to filter. non-matching objects will be
	 *            removed from the list
	 * @throws NodeException
	 */
	default void filter(List<? extends T> objects) throws NodeException {
		// remove all objects from the given list, that do not match the filter
		for (Iterator<? extends T> i = objects.iterator(); i.hasNext();) {
			T object = i.next();

			if (!matches(object)) {
				i.remove();
			}
		}
	}
}
