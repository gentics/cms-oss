/*
 * PoolEmptyException.java
 *
 * Created on 28. August 2004, 09:45
 */

package com.gentics.lib.pooling;

/**
 * @author Dietmar
 */
public class PoolEmptyException extends Exception {
	public PoolEmptyException(String s) {
		super(s);
	}

	public PoolEmptyException() {
		this("The Pool has reached its maximum amount of elements");
	}
}
