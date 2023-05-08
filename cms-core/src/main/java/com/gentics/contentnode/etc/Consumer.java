package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;

/**
 * Functional interface for a consumer that may throw a NodeException
 *
 * @param <T> parameter type
 */
@FunctionalInterface
public interface Consumer <T> {
	/**
	 * Consume the given parameter
	 * @param t parameter
	 * @throws NodeException
	 */
	void accept(T t) throws NodeException;
}
