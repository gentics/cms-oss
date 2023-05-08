package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;

/**
 * Functional interface for an operator that may throw a NodeException
 */
@FunctionalInterface
public interface Operator {
	/**
	 * Perform the operation
	 * @throws NodeException
	 */
	void operate() throws NodeException;
}
