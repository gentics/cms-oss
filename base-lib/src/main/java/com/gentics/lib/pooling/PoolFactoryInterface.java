/*
 * PoolFactoryInterface.java
 *
 * Created on 30. August 2004, 20:59
 */

package com.gentics.lib.pooling;

/**
 * @author Dietmar
 */
public interface PoolFactoryInterface {
	public Poolable createObject() throws PoolingException;

	public void reinitObject(Poolable object) throws PoolingException;

	public void destroyObject(Poolable object) throws PoolingException;
}
