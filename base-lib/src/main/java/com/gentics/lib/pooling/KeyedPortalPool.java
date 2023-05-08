/*
 * @author norbert
 * @date 26.04.2006
 * @version $Id: KeyedPortalPool.java,v 1.1 2006-04-27 10:12:45 norbert Exp $
 */
package com.gentics.lib.pooling;

/**
 * Interface for keyed Portal Pools. TODO: add more methods here
 */
public interface KeyedPortalPool {

	/**
	 * Borrow an object from the pool with the given key. The borrowed object
	 * must be returned to the pool via {@link #returnObject(Object, Object)}
	 * after usage.
	 * @param key key of the borrowed object
	 * @return borrowed object
	 * @throws PortalPoolException
	 */
	Object borrowObject(Object key) throws PortalPoolException;

	/**
	 * Return a borrowed object to the pool. The object must have been borrowed
	 * via {@link #borrowObject(Object)} before.
	 * @param key key of the borrowed object
	 * @param obj object to return to the pool
	 * @throws PortalPoolException
	 */
	void returnObject(Object key, Object obj) throws PortalPoolException;
}
