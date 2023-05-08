package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;

/**
 * Functional interface for a function that may throw a NodeException
 *
 * @param <T> function parameter type
 * @param <R> result type
 */
@FunctionalInterface
public interface Function<T, R> {
	/**
	 * Apply the function
	 * @param t parameter
	 * @return result
	 * @throws NodeException
	 */
	R apply(T t) throws NodeException;
}
