package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;

/**
 * Functional interface for a consumer that may throw a NodeException
 *
 * @param <T> type of first parameter
 * @param <U> type of second parameter
 */
@FunctionalInterface
public interface BiConsumer<T, U> {
	/**
	 * Consume the given parameters
	 * @param t first parameter
	 * @param u second parameter
	 * @throws NodeException
	 */
	void accept(T t, U u) throws NodeException;
}
