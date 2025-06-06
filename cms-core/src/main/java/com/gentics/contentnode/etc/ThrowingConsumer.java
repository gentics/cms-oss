package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;

/**
 * Functional interface for a consumer that may throw a subclass of {@link Throwable}.
 *
 * @param <T> parameter type
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
	/**
	 * Consume the given parameter
	 * @param t parameter
	 * @throws NodeException
	 */
	void accept(T t) throws E;
}