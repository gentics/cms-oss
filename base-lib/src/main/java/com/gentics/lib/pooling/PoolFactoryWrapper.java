/*
 * @author norbert
 * @date 14.11.2005
 * @version $Id: PoolFactoryWrapper.java,v 1.1 2005-11-18 10:08:53 norbert Exp $
 */
package com.gentics.lib.pooling;

import org.apache.commons.pool.PoolableObjectFactory;

/**
 * Wrapper for a PoolableObjectFactory of jakarta-commons-pool
 * @author norbert
 */
public class PoolFactoryWrapper implements PoolableObjectFactory {

	/**
	 * the wrapped factory
	 */
	protected PoolFactoryInterface wrappedFactory;

	/**
	 * Create an instance of the PoolFactoryWrapper
	 * @param wrappedFactory wrapped factory
	 */
	public PoolFactoryWrapper(PoolFactoryInterface wrappedFactory) {
		super();
		this.wrappedFactory = wrappedFactory;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
	 */
	public Object makeObject() throws Exception {
		return wrappedFactory.createObject();
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(java.lang.Object)
	 */
	public void destroyObject(Object obj) throws Exception {
		if (obj instanceof Poolable) {
			wrappedFactory.destroyObject((Poolable) obj);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool.PoolableObjectFactory#validateObject(java.lang.Object)
	 */
	public boolean validateObject(Object obj) {
		// default implementation, since PoolFactoryInterface does not have equivalent methods
		return true;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool.PoolableObjectFactory#activateObject(java.lang.Object)
	 */
	public void activateObject(Object obj) throws Exception {
		if (obj instanceof Poolable) {
			wrappedFactory.reinitObject((Poolable) obj);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool.PoolableObjectFactory#passivateObject(java.lang.Object)
	 */
	public void passivateObject(Object obj) throws Exception {// default implementation, since PoolFactoryInterface does not have equivalent methods
	}
}
