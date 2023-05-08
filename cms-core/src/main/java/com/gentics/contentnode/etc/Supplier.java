package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;

/**
 * Functional interface for a supplier that may throw a NodeException
 *
 * @param <R> result type
 */
@FunctionalInterface
public interface Supplier <R> {
	/**
	 * Return the supplied result
	 * @return result
	 * @throws NodeException
	 */
	R supply() throws NodeException;
}
