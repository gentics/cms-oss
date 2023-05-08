/*
 * @author norbert
 * @date 26.04.2006
 * @version $Id: CommonsKeyedPortalPool.java,v 1.1 2006-04-27 10:12:45 norbert Exp $
 */
package com.gentics.lib.pooling;

import org.apache.commons.pool.KeyedObjectPool;

/**
 * Implementation of a {@link com.gentics.lib.pooling.KeyedPortalPool} based on
 * the Jakarta Commons Pooling. This implementation just wraps the methods of
 * the interface KeyedPortalPool to methods of the wrapped Jakarta Commons Pool
 */
public class CommonsKeyedPortalPool implements KeyedPortalPool {

	/**
	 * Wrapped Commons Pool
	 */
	private KeyedObjectPool keyedObjectPool;

	/**
	 * Create a KeyedPortalPool
	 * @param keyedObjectPool wrapped pool
	 */
	public CommonsKeyedPortalPool(KeyedObjectPool keyedObjectPool) {
		this.keyedObjectPool = keyedObjectPool;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.pooling.KeyedPortalPool#borrowObject(java.lang.Object)
	 */
	public Object borrowObject(Object key) throws PortalPoolException {
		try {
			return keyedObjectPool.borrowObject(key);
		} catch (Exception ex) {
			throw new PortalPoolException(ex);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.pooling.KeyedPortalPool#returnObject(java.lang.Object, java.lang.Object)
	 */
	public void returnObject(Object key, Object obj) throws PortalPoolException {
		try {
			keyedObjectPool.returnObject(key, obj);
		} catch (Exception ex) {
			throw new PortalPoolException(ex);
		}
	}
}
