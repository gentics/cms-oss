/*
 * @author norbert
 * @date 26.04.2006
 * @version $Id: PortalPool.java,v 1.1 2006-04-27 10:12:45 norbert Exp $
 */
package com.gentics.lib.pooling;

/**
 * Interface for not-keyed portal pools TODO: implement more methods here
 */
public interface PortalPool {

	/**
	 * Borrow an object from the pool. The borrowed object must be returned to
	 * the pool via {@link #returnObject(Object)} after usage.
	 * @return the borrowed object
	 * @throws PortalPoolException
	 */
	Object borrowObject() throws PortalPoolException;

	/**
	 * Return a borrowed object to the pool. The object must have been borrowed
	 * via {@link #borrowObject()} before.
	 * @param obj object to return to the pool
	 * @throws PortalPoolException
	 */
	void returnObject(Object obj) throws PortalPoolException;
}
