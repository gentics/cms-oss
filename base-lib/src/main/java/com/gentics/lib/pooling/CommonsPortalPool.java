/*
 * @author norbert
 * @date 26.04.2006
 * @version $Id: CommonsPortalPool.java,v 1.1 2006-04-27 10:12:45 norbert Exp $
 */
package com.gentics.lib.pooling;

import org.apache.commons.pool.ObjectPool;

/**
 * Implementation of a {@link com.gentics.lib.pooling.PortalPool} based on
 * the Jakarta Commons Pooling. This implementation just wraps the methods of
 * the interface PortalPool to methods of the wrapped Jakarta Commons Pool
 */
public class CommonsPortalPool implements PortalPool {

	/**
	 * Wrapped Commons Pool
	 */
	private ObjectPool objectPool;

	/**
	 * Create an instance of the wrapper
	 * @param objectPool wrapped Commons Pool
	 */
	public CommonsPortalPool(ObjectPool objectPool) {
		this.objectPool = objectPool;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.pooling.PortalPool#borrowObject()
	 */
	public Object borrowObject() throws PortalPoolException {
		try {
			return objectPool.borrowObject();
		} catch (Exception ex) {
			throw new PortalPoolException(ex);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.pooling.PortalPool#returnObject(java.lang.Object)
	 */
	public void returnObject(Object obj) throws PortalPoolException {
		try {
			objectPool.returnObject(obj);
		} catch (Exception ex) {
			throw new PortalPoolException(ex);
		}
	}
}
