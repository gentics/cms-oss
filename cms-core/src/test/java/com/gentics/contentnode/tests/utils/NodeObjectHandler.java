package com.gentics.contentnode.tests.utils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject;

/**
 * Functional interface for object handlers
 *
 * @param <T> Object class
 */
@FunctionalInterface
public interface NodeObjectHandler<T extends NodeObject> {
	/**
	 * Handle the object
	 * @param object object
	 * @throws NodeException
	 */
	void handle(T object) throws NodeException;
}
