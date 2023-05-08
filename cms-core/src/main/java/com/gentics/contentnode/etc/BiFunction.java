package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;

/**
 * Functional interface for a function that may throw a NodeException
 *
 * @param <T> function first parameter type
 * @param <U> function second paramter type
 * @param <R> result type
 */
@FunctionalInterface
public interface BiFunction<T, U, R> {
	/**
	 * Apply the function
	 * @param t first parameter
	 * @param u second parameter
	 * @return result
	 * @throws NodeException
	 */
	R apply(T t, U u) throws NodeException;
}
