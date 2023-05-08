/*
 * PoolInterface.java
 *
 * Created on 26. August 2004, 23:46
 */

package com.gentics.lib.pooling;

/**
 * @author Dietmar
 */
public interface PoolInterface {
	public Poolable getInstance() throws PoolEmptyException, PoolingException,
				IllegalAccessException;

	public void releaseInstance(Poolable PooledObject) throws NotPoolObjectException;

	public boolean belongsToPool(Poolable Object);

	public void removeAll();

	/* @deprecated */

	/* public Class getPooledClass(); */
}
