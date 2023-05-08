package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;

/**
 * Functional interface for a function that may throw a NodeException
 *
 * @param <T> function first parameter type
 * @param <U> function second parameter type
 * @param <V> function third parameter type
 * @param <R> result type
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {
	/**
	 * Apply the function
	 * @param t first parameter
	 * @param u second parameter
	 * @param v third parameter
	 * @return result
	 * @throws NodeException
	 */
	R apply(T t, U u, V v) throws NodeException;
}
